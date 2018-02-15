/*
 * Copyright (C) 2011 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.caliper.worker.connection;

import static com.google.common.base.Preconditions.checkState;

import com.google.caliper.bridge.OpenedSocket;
import com.google.common.util.concurrent.AbstractIdleService;
import java.io.IOException;
import java.io.Serializable;
import java.net.InetSocketAddress;
import java.nio.channels.SocketChannel;
import javax.inject.Inject;
import javax.inject.Singleton;

/**
 * A service that opens and maintains a socket connection to the another process and provides the
 * worker's interface for communicating with the that process.
 *
 * @author Colin Decker
 */
@Singleton
public final class ClientConnectionService extends AbstractIdleService {

  private final InetSocketAddress clientAddress;

  private volatile SocketChannel channel;
  private volatile OpenedSocket.Writer writer;
  private volatile OpenedSocket.Reader reader;

  @Inject
  ClientConnectionService(InetSocketAddress clientAddress) {
    this.clientAddress = clientAddress;
  }

  @Override
  protected void startUp() throws IOException {
    channel = SocketChannel.open();
    channel.connect(clientAddress);

    OpenedSocket openedSocket = OpenedSocket.fromSocket(channel);
    writer = openedSocket.writer();
    reader = openedSocket.reader();
  }

  @Override
  protected void shutDown() throws IOException {
    try {
      try {
        reader.close();
      } finally {
        writer.close();
      }
    } finally {
      channel.close();
    }
  }

  /** Sends the given messages to the client. */
  public void send(Serializable... messages) throws IOException {
    checkState(isRunning(), "send() may only be called when the service is running");
    for (Serializable message : messages) {
      writer.write(message);
    }
    writer.flush();
  }

  /** Blocks to receive a message sent by the client. */
  public Object receive() throws IOException {
    checkState(isRunning(), "receive() may only be called when the service is running");
    return reader.read();
  }
}
