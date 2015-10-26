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

package com.google.caliper.runner;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;

import com.google.caliper.bridge.LogMessage;
import com.google.caliper.bridge.OpenedSocket;
import com.google.caliper.bridge.StopMeasurementLogMessage;
import com.google.caliper.model.Measurement;
import com.google.caliper.runner.StreamService.StreamItem.Kind;
import com.google.caliper.util.Parser;
import com.google.common.base.MoreObjects;
import com.google.common.base.MoreObjects.ToStringHelper;
import com.google.common.collect.Queues;
import com.google.common.io.Closeables;
import com.google.common.io.LineReader;
import com.google.common.util.concurrent.AbstractService;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.MoreExecutors;
import com.google.common.util.concurrent.Service; // for javadoc
import com.google.common.util.concurrent.Service.State; // for javadoc
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.google.common.util.concurrent.Uninterruptibles;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.Serializable;
import java.nio.charset.Charset;
import java.text.ParseException;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Logger;

import javax.annotation.Nullable;
import javax.inject.Inject;

/**
 * A {@link Service} that establishes a connection over a socket to a process and then allows 
 * multiplexed access to the processes' line oriented output over the socket and the standard 
 * process streams (stdout and stderr) as well as allowing data to be written over the socket.
 * 
 * <p>The {@linkplain State states} of this service are as follows:
 * <ul>
 *   <li>{@linkplain State#NEW NEW} : Idle state, no reading or writing is allowed.
 *   <li>{@linkplain State#STARTING STARTING} : Streams are being opened
 *   <li>{@linkplain State#RUNNING RUNNING} : At least one stream is still open or the writer has 
 *       not been closed yet.
 *   <li>{@linkplain State#STOPPING STOPPING} : All streams have closed but some threads may still 
 *       be running.
 *   <li>{@linkplain State#TERMINATED TERMINATED} : Idle state, all streams are closed
 *   <li>{@linkplain State#FAILED FAILED} : The service will transition to failed if it encounters
 *       any errors while reading from or writing to the streams, service failure will also cause 
 *       the worker process to be forcibly shutdown and {@link #readItem(long, TimeUnit)}, 
 *       {@link #closeWriter()} and {@link #sendMessage(Serializable)} will start throwing 
 *       IllegalStateExceptions. 
 * </ul>
 */
@TrialScoped final class StreamService extends AbstractService {
  /** How long to wait for a process that should be exiting to actually exit. */
  private static final int SHUTDOWN_WAIT_MILLIS = 10;

  private static final Logger logger = Logger.getLogger(StreamService.class.getName());
  private static final StreamItem TIMEOUT_ITEM = new StreamItem(Kind.TIMEOUT, null);

  /** The final item that will be sent down the stream. */
  static final StreamItem EOF_ITEM = new StreamItem(Kind.EOF, null);

  private final ListeningExecutorService streamExecutor = MoreExecutors.listeningDecorator(
      Executors.newCachedThreadPool(new ThreadFactoryBuilder().setDaemon(true).build()));
  private final BlockingQueue<StreamItem> outputQueue = Queues.newLinkedBlockingQueue();
  private final WorkerProcess worker;
  private volatile Process process;
  private final Parser<LogMessage> logMessageParser;
  private final TrialOutputLogger trialOutput;
  
  /** 
   * This represents the number of open streams from the users perspective.  i.e. can you still
   * write to the socket and read items.
   * 
   * <p>This is decremented when either the socket is closed for writing or the EOF_ITEM has been
   * read by the user.
   */
  private final AtomicInteger openStreams = new AtomicInteger();

  /** 
   * Used to track how many read streams are open so we can correctly set the EOF_ITEM onto the 
   * queue.
   */
  private final AtomicInteger runningReadStreams = new AtomicInteger();
  private OpenedSocket.Writer socketWriter;

  @Inject StreamService(WorkerProcess worker,
      Parser<LogMessage> logMessageParser, 
      TrialOutputLogger trialOutput) {
    this.worker = worker;
    this.logMessageParser = logMessageParser;
    this.trialOutput = trialOutput;
  }

  @Override protected void doStart() {
    try {
      // TODO(lukes): write the commandline to the trial output file?
      process = worker.startWorker();
    } catch (IOException e) {
      notifyFailed(e);
      return;
    }
    // Failsafe kill the process and the executor service.
    // If the process has already exited cleanly, this will be a no-op.
    addListener(new Listener() {
      @Override public void starting() {}
      @Override public void running() {}
      @Override public void stopping(State from) {}
      @Override public void terminated(State from) {
        cleanup();
      }
      @Override public void failed(State from, Throwable failure) {
        cleanup();
      }

      void cleanup() {
        streamExecutor.shutdown();
        process.destroy();
        try {
          streamExecutor.awaitTermination(10, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
          Thread.currentThread().interrupt();
        }
        streamExecutor.shutdownNow();
      }
    }, MoreExecutors.directExecutor());
    // You may be thinking as you read this "Yo dawg, what if IOExceptions rain from the sky?" 
    // If a stream we are reading from throws an IOException then we fail the entire Service. This
    // will cause the worker to be killed (if its not dead already) and the various StreamReaders to
    // be interrupted (eventually).

    // use the default charset because worker streams will use the default for output
    Charset processCharset = Charset.defaultCharset();
    runningReadStreams.addAndGet(2);
    openStreams.addAndGet(1);
    streamExecutor.submit(
        threadRenaming("worker-stderr", 
            new StreamReader("stderr", 
                new InputStreamReader(process.getErrorStream(), processCharset))));
    streamExecutor.submit(
        threadRenaming("worker-stdout",
            new StreamReader("stdout", 
                new InputStreamReader(process.getInputStream(), processCharset))));
    worker.socketFuture().addListener(
        new Runnable() {
          @Override public void run() {
            try {
              OpenedSocket openedSocket =
                  Uninterruptibles.getUninterruptibly(worker.socketFuture());
              logger.fine("successfully opened the pipe from the worker");
              socketWriter = openedSocket.writer();
              runningReadStreams.addAndGet(1);
              openStreams.addAndGet(1);
              streamExecutor.submit(threadRenaming("worker-socket",
                  new SocketStreamReader(openedSocket.reader())));
            } catch (ExecutionException e) {
              notifyFailed(e.getCause());
            }
          }
        },
        MoreExecutors.directExecutor());
    notifyStarted();
  }
  
  /**
   * Reads a {@link StreamItem} from one of the streams waiting for one to become available if 
   * necessary.
   */
  StreamItem readItem(long timeout, TimeUnit unit) throws InterruptedException {
    checkState(isRunning(), "Cannot read items from a %s StreamService", state());
    StreamItem line = outputQueue.poll(timeout, unit);
    if (line == EOF_ITEM) {
      closeStream();
    }
    return (line == null) ? TIMEOUT_ITEM : line;
  }
  
  /**
   * Write a line of data to the worker process over the socket.
   *
   * <p>N.B. Writing data via {@link #sendMessage(Serializable)} is only valid once the underlying
   * socket has been opened.  This should be fine assuming that socket writes are only in response
   * to socket reads (which is currently the case), so there is no way that a write could happen
   * prior to the socket being opened.
  */
  void sendMessage(Serializable message) throws IOException {
    checkState(isRunning(), "Cannot read items from a %s StreamService", state());
    checkState(socketWriter != null, "Attempted to write to the socket before it was opened.");
    try {
      socketWriter.write(message);
      // We need to flush since this is a back and forth lockstep protocol, buffering can cause 
      // deadlock! 
      socketWriter.flush();
    } catch (IOException e) {
      Closeables.close(socketWriter, true);
      notifyFailed(e);
      throw e;
    }
  }
  
  /** Closes the socket writer. */
  void closeWriter() throws IOException {
    checkState(isRunning(), "Cannot read items from a %s StreamService", state());
    checkState(socketWriter != null, "Attempted to close the socket before it was opened.");
    try {
      socketWriter.close();
    } catch (IOException e) {
      notifyFailed(e);
      throw e;
    }
    closeStream();
  }
  
  @Override protected void doStop() {
    if (openStreams.get() > 0) {
      // This means stop was called on us externally and we are still reading/writing, just log a
      // warning and do nothing
      logger.warning("Attempting to stop the stream service with streams still open");
    }
    final ListenableFuture<Integer> processFuture = streamExecutor.submit(new Callable<Integer>() {
      @Override public Integer call() throws Exception {
        return process.waitFor();
      }
    });
    // Experimentally, even with well behaved processes there is some time between when all streams
    // are closed as part of process shutdown and when the process has exited. So to not fail 
    // flakily when shutting down normally we need to do a timed wait
    streamExecutor.submit(new Callable<Void>() {
      @Override public Void call() throws Exception {
        boolean threw = true;
        try {
          if (processFuture.get(SHUTDOWN_WAIT_MILLIS, TimeUnit.MILLISECONDS) == 0) {
            notifyStopped();
          } else {
            notifyFailed(
                new Exception("Process failed to stop cleanly. Exit code: " + process.waitFor()));
          }
          threw = false;
        } finally {
          processFuture.cancel(true);  // we don't need it anymore
          if (threw) {
            process.destroy();
            notifyFailed(
                new Exception("Process failed to stop cleanly and was forcibly killed. Exit code: " 
                    + process.waitFor()));
          }
        }
        return null;
      }
    });
  }
  
  private void closeStream() {
    if (openStreams.decrementAndGet() == 0) {
      stopAsync();
    }
  }
  
  private void closeReadStream() {
    if (runningReadStreams.decrementAndGet() == 0) {
      outputQueue.add(EOF_ITEM);
    }
  }

  /** An item read from one of the streams. */
  static class StreamItem {
    enum Kind {
      /** This indicates that it is the last item. */
      EOF,
      /** This indicates that reading the item timed out. */
      TIMEOUT,
      /** This indicates that this item has content. */
      DATA;
    }
    
    @Nullable private final LogMessage logMessage;
    private final Kind kind;
    
    private StreamItem(LogMessage line) {
      this(Kind.DATA, checkNotNull(line));
    }
    
    private StreamItem(Kind state, @Nullable LogMessage logMessage) {
      this.logMessage = logMessage;
      this.kind = state;
    }
    
    /** Returns the content.  This is only valid if {@link #kind()} return {@link Kind#DATA}. */
    LogMessage content() {
      checkState(kind == Kind.DATA, "Only data lines have content: %s", this);
      return logMessage;
    }
    
    Kind kind() {
      return kind;
    }
    
    @Override public String toString() {
      ToStringHelper helper = MoreObjects.toStringHelper(StreamItem.class);
      if (kind == Kind.DATA) {
        helper.addValue(logMessage);
      } else {
        helper.addValue(kind);
      }
      return helper.toString();
    }
  }
  
  /** Returns a callable that renames the the thread that the given callable runs in. */
  private static <T> Callable<T> threadRenaming(final String name, final Callable<T> callable) {
    checkNotNull(name);
    checkNotNull(callable);
    return new Callable<T>() {
      @Override public T call() throws Exception {
        Thread currentThread = Thread.currentThread();
        String oldName = currentThread.getName();
        currentThread.setName(name);
        try {
          return callable.call();
        } finally {
          currentThread.setName(oldName);
        }
      }
    };
  }

  /**
   * A background task that reads lines of text from a {@link Reader} and puts them onto a 
   * {@link BlockingQueue}.
   */
  private final class StreamReader implements Callable<Void> {
    final Reader reader;
    final String streamName;

    StreamReader(String streamName, Reader reader) {
      this.streamName = streamName;
      this.reader = reader;
    }
    
    @Override public Void call() throws IOException, InterruptedException, ParseException {
      LineReader lineReader = new LineReader(reader);
      boolean threw = true;
      try {
        String line;
        while ((line = lineReader.readLine()) != null) {
          trialOutput.log(streamName, line);
          LogMessage logMessage = logMessageParser.parse(line);
          if (logMessage != null) {
            outputQueue.put(new StreamItem(logMessage));
          }
        }
        threw = false;
      } catch (Exception e) {
        notifyFailed(e);
      } finally {
        closeReadStream();
        Closeables.close(reader, threw);
      }
      return null;
    }
  }
  
  /**
   * A background task that reads lines of text from a {@link OpenedSocket.Reader} and puts them
   * onto a {@link BlockingQueue}.
   */
  private final class SocketStreamReader implements Callable<Void> {
    final OpenedSocket.Reader reader;

    SocketStreamReader(OpenedSocket.Reader reader) {
      this.reader = reader;
    }
    
    @Override public Void call() throws IOException, InterruptedException, ParseException {
      boolean threw = true;
      try {
        Object obj;
        while ((obj = reader.read()) != null) {
          if (obj instanceof String) {
            log(obj.toString());
            continue;
          } 
          LogMessage message = (LogMessage) obj;
          if (message instanceof StopMeasurementLogMessage) {
            // TODO(lukes): how useful are these messages?  They seem like leftover debugging info
            for (Measurement measurement : ((StopMeasurementLogMessage) message).measurements()) {
              log(String.format("I got a result! %s: %f%s%n",
                  measurement.description(),
                  measurement.value().magnitude() / measurement.weight(), 
                  measurement.value().unit()));
            }
          }
          outputQueue.put(new StreamItem(message));
        }
        threw = false;
      } catch (Exception e) {
        notifyFailed(e);
      } finally {
        closeReadStream();
        Closeables.close(reader, threw);
      }
      return null;
    }

    private void log(String text) {
      trialOutput.log("socket", text);
    }
  }
}
