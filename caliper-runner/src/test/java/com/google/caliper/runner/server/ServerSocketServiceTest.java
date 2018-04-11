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

package com.google.caliper.runner.server;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import com.google.caliper.bridge.OpenedSocket;
import com.google.caliper.util.Uuids;
import com.google.common.util.concurrent.ListenableFuture;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.Socket;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/** Tests for {@link ServerSocketService}. */
@RunWith(JUnit4.class)

public class ServerSocketServiceTest {

  private final ServerSocketService service = new ServerSocketService();
  private int port;

  private ExecutorService executor;

  @Before
  public void before() {
    service.startAsync().awaitRunning();
    port = service.getPort();
    executor = Executors.newSingleThreadExecutor();
  }

  @After
  public void after() {
    service.stopAsync().awaitTerminated();
    executor.shutdownNow();
  }

  @Test
  public void getConnectionId_requestComesInFirst() throws Exception {
    UUID id = UUID.randomUUID();
    ListenableFuture<OpenedSocket> pendingServerConnection = service.getConnection(id);
    assertFalse(pendingServerConnection.isDone());
    Future<OpenedSocket> clientSocketFuture = createOpenedSocket(openConnectionAndIdentify(id));
    // Assert that the ends are hooked up to each other
    assertEndsConnected(clientSocketFuture.get(), pendingServerConnection.get());
  }

  @Test
  public void getConnectionIdTwice_acceptComesFirst() throws Exception {
    UUID id = UUID.randomUUID();
    Future<OpenedSocket> clientSocketFuture = createOpenedSocket(openConnectionAndIdentify(id));
    ListenableFuture<OpenedSocket> pendingServerConnection = service.getConnection(id);
    OpenedSocket clientSocket = clientSocketFuture.get();
    OpenedSocket serverSocket = pendingServerConnection.get();
    assertEndsConnected(clientSocket, serverSocket);
    try {
      // the second request is an error
      service.getConnection(id).get();
      fail();
    } catch (IllegalStateException expected) {
    }
  }

  @Test
  public void getConnectionStoppedService() throws Exception {
    UUID id = UUID.randomUUID();
    ListenableFuture<OpenedSocket> pendingServerConnection = service.getConnection(id);
    assertFalse(pendingServerConnection.isDone());
    service.stopAsync().awaitTerminated();
    assertTrue(pendingServerConnection.isDone());

    try {
      pendingServerConnection.get();
      fail();
    } catch (ExecutionException e) {
      assertEquals("The socket has been closed", e.getCause().getMessage());
    }

    try {
      service.getConnection(UUID.randomUUID());
      fail();
    } catch (IllegalStateException expected) {
    }
  }

  @Test
  public void getInputStream() throws Exception {
    UUID id = UUID.randomUUID();
    ListenableFuture<InputStream> pendingServerConnection = service.getInputStream(id);
    assertFalse(pendingServerConnection.isDone());

    Socket clientSocket = openConnectionAndIdentify(id);
    // This should work now that the client has connected and sent its ID
    InputStream serverInputStream = pendingServerConnection.get();

    OutputStream clientOutputStream = clientSocket.getOutputStream();
    byte[] bytes = new byte[] {1, 2, 3, 4};
    clientOutputStream.write(bytes);

    byte[] buf = new byte[bytes.length];
    int bytesRead = 0;
    int r;
    while (bytesRead < buf.length
        && (r = serverInputStream.read(buf, bytesRead, buf.length - bytesRead)) != -1) {
      bytesRead += r;
    }
    assertThat(buf).isEqualTo(bytes);

    clientOutputStream.close();
    serverInputStream.close();
  }

  private Socket openClientConnection() throws IOException {
    return new Socket(InetAddress.getLoopbackAddress(), port);
  }

  /** Opens a connection to the service and identifies itself using the id. */
  private Socket openConnectionAndIdentify(UUID id) throws IOException {
    Socket socket = openClientConnection();
    socket.getOutputStream().write(Uuids.toBytes(id).array());
    socket.getOutputStream().flush();
    return socket;
  }

  /**
   * Creates an {@code OpenedSocket} wrapped for the given socket. This needs to be done on a
   * separate thread to prevent some dumb stuff with {@code OpenedSocket}'s initialization of {@code
   * ObjectInput/OutputStream}s that only happens when two {@code OpenedSocket}s connected to each
   * other are being created on the same thread. The two ends won't even be in the same process,
   * much less the same thread, in real use.
   */
  private Future<OpenedSocket> createOpenedSocket(Socket socket) {
    return executor.submit(
        new Callable<OpenedSocket>() {
          @Override
          public OpenedSocket call() throws IOException {
            return OpenedSocket.fromSocket(socket);
          }
        });
  }

  private void assertEndsConnected(OpenedSocket clientSocket, OpenedSocket serverSocket)
      throws IOException {
    serverSocket.writer().write("hello client!");
    serverSocket.writer().flush(); // necessary to prevent deadlock
    assertEquals("hello client!", clientSocket.reader().read());

    clientSocket.writer().write("hello server!");
    clientSocket.writer().flush(); // ditto
    assertEquals("hello server!", serverSocket.reader().read());
  }
}
