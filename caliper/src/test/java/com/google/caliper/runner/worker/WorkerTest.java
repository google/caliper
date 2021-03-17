/*
 * Copyright (C) 2013 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */

package com.google.caliper.runner.worker;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkState;
import static java.util.concurrent.TimeUnit.MINUTES;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import com.google.caliper.bridge.LogMessage;
import com.google.caliper.bridge.OpenedSocket;
import com.google.caliper.runner.target.Device;
import com.google.caliper.runner.target.LocalDevice;
import com.google.caliper.runner.testing.FakeWorkerSpec;
import com.google.caliper.runner.testing.FakeWorkers;
import com.google.caliper.runner.testing.FakeWorkers.DummyLogMessage;
import com.google.caliper.runner.worker.Worker.StreamItem;
import com.google.caliper.runner.worker.Worker.StreamItem.Kind;
import com.google.caliper.util.Parser;
import com.google.common.collect.Sets;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListenableFutureTask;
import com.google.common.util.concurrent.MoreExecutors;
import com.google.common.util.concurrent.Service.Listener;
import com.google.common.util.concurrent.Service.State;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.ServerSocket;
import java.net.SocketException;
import java.text.ParseException;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/** Tests for {@link Worker}. */
@RunWith(JUnit4.class)

public class WorkerTest {

  private ServerSocket serverSocket;
  private final StringWriter writer = new StringWriter();
  private final PrintWriter stdout = new PrintWriter(writer, true);
  private final Parser<LogMessage> parser =
      new Parser<LogMessage>() {
        @Override
        public LogMessage parse(final CharSequence text) throws ParseException {
          return new DummyLogMessage(text.toString());
        }
      };
  private final Device device = LocalDevice.builder().build();

  private Worker worker;
  private final CountDownLatch terminalLatch = new CountDownLatch(1);

  @Before
  public void openSocket() throws IOException {
    serverSocket = new ServerSocket(0);
  }

  @Before
  public void startDevice() {
    device.startAsync().awaitRunning();
  }

  @After
  public void closeSocket() throws IOException {
    serverSocket.close();
  }

  @After
  public void stopDevice() {
    device.stopAsync().awaitTerminated();
  }

  @After
  public void stopWorker() {
    if (worker != null && worker.state() != State.FAILED && worker.state() != State.TERMINATED) {
      worker.stopAsync().awaitTerminated();
    }
  }

  @Test
  public void testReadOutput() throws Exception {
    makeWorker(FakeWorkers.PrintClient.class, "foo", "bar");
    worker.startAsync().awaitRunning();
    StreamItem item1 = readItem();
    assertEquals(Kind.DATA, item1.kind());
    Set<String> lines = Sets.newHashSet();
    lines.add(item1.content().toString());
    StreamItem item2 = readItem();
    assertEquals(Kind.DATA, item2.kind());
    lines.add(item2.content().toString());
    assertEquals(Sets.newHashSet("foo", "bar"), lines);
    assertEquals(State.RUNNING, worker.state());
    StreamItem item3 = readItem();
    assertEquals(Kind.EOF, item3.kind());
    awaitStopped();
    assertTerminated();
  }

  @Test
  public void failingProcess() throws Exception {
    makeWorker(FakeWorkers.Exit.class, "1");
    worker.startAsync().awaitRunning();
    assertEquals(Kind.EOF, readItem().kind());
    awaitStopped();
    assertEquals(State.FAILED, worker.state());
  }

  @Test
  public void processDoesntExit() throws Exception {
    // close all fds and then sleep
    makeWorker(FakeWorkers.CloseAndSleep.class);
    worker.startAsync().awaitRunning();
    assertEquals(Kind.EOF, readItem().kind());
    awaitStopped();
    assertEquals(State.FAILED, worker.state());
  }

  @Test
  public void testSocketInputOutput() throws Exception {
    int localport = serverSocket.getLocalPort();
    // read from the socket and echo it back
    makeWorker(FakeWorkers.SocketEchoClient.class, Integer.toString(localport));

    worker.startAsync().awaitRunning();
    assertEquals(new DummyLogMessage("start"), readItem().content());
    worker.sendMessage(new DummyLogMessage("hello socket world"));
    assertEquals(new DummyLogMessage("hello socket world"), readItem().content());
    worker.closeWriter();
    assertEquals(State.RUNNING, worker.state());
    StreamItem nextItem = readItem();
    assertEquals("Expected EOF " + nextItem, Kind.EOF, nextItem.kind());
    awaitStopped();
    assertTerminated();
  }

  @Test
  public void testSocketClosesBeforeProcess() throws Exception {
    int localport = serverSocket.getLocalPort();
    // read from the socket and echo it back
    makeWorker(FakeWorkers.SocketEchoClient.class, Integer.toString(localport), "foo");
    worker.startAsync().awaitRunning();
    assertEquals(new DummyLogMessage("start"), readItem().content());
    worker.sendMessage(new DummyLogMessage("hello socket world"));
    assertEquals(new DummyLogMessage("hello socket world"), readItem().content());
    worker.closeWriter();

    assertEquals("foo", readItem().content().toString());

    assertEquals(State.RUNNING, worker.state());
    assertEquals(Kind.EOF, readItem().kind());
    awaitStopped();
    assertTerminated();
  }

  @Test
  public void failsToAcceptConnection() throws Exception {
    serverSocket.close(); // This will force serverSocket.accept to throw a SocketException
    makeWorker(FakeWorkers.Sleeper.class, Long.toString(MINUTES.toMillis(10)));

    worker.startAsync();

    // awaitRunning() may succeed depending on the timing of the thread that calls
    // serverSocket.accept(), but the service should fail shortly after regardless. So here we just
    // wait for the service to stop.
    awaitStopped();

    assertEquals(SocketException.class, worker.failureCause().getClass());
  }

  /** Reads an item, asserting that there was no timeout. */
  private StreamItem readItem() throws InterruptedException {
    StreamItem item = worker.readItem(10, SECONDS);
    assertNotSame("Timed out while reading item from worker", Kind.TIMEOUT, item.kind());
    return item;
  }

  /** Wait for the worker to reach a terminal state without calling stop. */
  private void awaitStopped() throws InterruptedException {
    assertTrue(terminalLatch.await(10, SECONDS));
  }

  private void assertTerminated() {
    State state = worker.state();
    if (state != State.TERMINATED) {
      if (state == State.FAILED) {
        throw new AssertionError(worker.failureCause());
      }
      fail("Expected worker to be terminated but was: " + state);
    }
  }

  @SuppressWarnings("resource")
  private void makeWorker(Class<?> main, String... args) {
    checkState(worker == null, "You can only make one Worker per test");
    final WorkerSpec spec = FakeWorkerSpec.builder(main).setArgs(args).build();

    WorkerOutputLogger output =
        new WorkerOutputLogger(
            new WorkerOutputFactory() {
              @Override
              public FileAndWriter getOutputFile(String fileName) throws FileNotFoundException {
                checkArgument(fileName.equals("worker-" + spec.id() + ".log"));
                return new FileAndWriter(new File("/tmp/not-a-file"), stdout);
              }

              @Override
              public void persistFile(File f) {
                throw new UnsupportedOperationException();
              }
            },
            spec);
    try {
      // normally the TrialRunLoop opens/closes the logger
      output.open();
    } catch (IOException e) {
      throw new RuntimeException(e);
    }

    worker = new Worker(spec, device, getSocketFuture(), parser, output);
    worker.addListener(
        new Listener() {
          @Override
          public void terminated(State from) {
            terminalLatch.countDown();
          }

          @Override
          public void failed(State from, Throwable failure) {
            terminalLatch.countDown();
          }
        },
        MoreExecutors.directExecutor());
  }

  private ListenableFuture<OpenedSocket> getSocketFuture() {
    ListenableFutureTask<OpenedSocket> openSocketTask =
        ListenableFutureTask.create(
            new Callable<OpenedSocket>() {
              @Override
              public OpenedSocket call() throws Exception {
                return OpenedSocket.fromSocket(serverSocket.accept());
              }
            });
    // N.B. this thread will block on serverSocket.accept until a connection is accepted or the
    // socket is closed, so no matter what this thread will die with the test.
    Thread opener = new Thread(openSocketTask, "SocketOpener");
    opener.setDaemon(true);
    opener.start();
    return openSocketTask;
  }
}
