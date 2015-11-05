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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import com.google.caliper.bridge.OpenedSocket;
import com.google.caliper.bridge.StartupAnnounceMessage;
import com.google.common.util.concurrent.ListenableFuture;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;
import java.util.UUID;
import java.util.concurrent.ExecutionException;

/**
 * Tests for {@link ServerSocketService}.
 */
@RunWith(JUnit4.class)

public class ServerSocketServiceTest {

  private final ServerSocketService service = new ServerSocketService();
  private int port;

  @Before public void startService() {
    service.startAsync().awaitRunning();
    port = service.getPort();
  }

  @After public void stopService() {
    service.stopAsync().awaitTerminated();
  }

  @Test public void getConnectionId_requestComesInFirst() throws Exception {
    UUID id = UUID.randomUUID();
    ListenableFuture<OpenedSocket> pendingServerConnection = service.getConnection(id);
    assertFalse(pendingServerConnection.isDone());
    OpenedSocket clientSocket = openConnectionAndIdentify(id);
    // Assert that the ends are hooked up to each other
    assertEndsConnected(clientSocket, pendingServerConnection.get());
  }

  @Test public void getConnectionIdTwice_acceptComesFirst() throws Exception {
    UUID id = UUID.randomUUID();
    OpenedSocket clientSocket = openConnectionAndIdentify(id);

    ListenableFuture<OpenedSocket> pendingServerConnection = service.getConnection(id);
    // wait for the service to fully initialize the connection
    OpenedSocket serverSocket = pendingServerConnection.get();
    assertEndsConnected(clientSocket, serverSocket);
    try {
      // the second request is an error
      service.getConnection(id).get();
      fail();
    } catch (IllegalStateException expected) {}
  }

  @Test public void getConnectionStoppedService() throws Exception {
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
    } catch (IllegalStateException expected) {}
  }

  private OpenedSocket openClientConnection() throws IOException {
    return OpenedSocket.fromSocket(new Socket(InetAddress.getLoopbackAddress(), port));
  }

  /**
   * Opens a connection to the service and identifies itself using the id.
   */
  private OpenedSocket openConnectionAndIdentify(UUID id) throws IOException {
    OpenedSocket clientSocket = openClientConnection();
    OpenedSocket.Writer writer = clientSocket.writer();
    writer.write(new StartupAnnounceMessage(id));
    writer.flush();
    return clientSocket;
  }

  private void assertEndsConnected(OpenedSocket clientSocket, OpenedSocket serverSocket)
      throws IOException {
    serverSocket.writer().write("hello client!");
    serverSocket.writer().flush();  // necessary to prevent deadlock
    assertEquals("hello client!", clientSocket.reader().read());

    clientSocket.writer().write("hello server!");
    clientSocket.writer().flush();  // ditto
    assertEquals("hello server!", serverSocket.reader().read());
  }
}
