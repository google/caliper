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

import static com.google.common.base.Charsets.UTF_8;
import static com.google.common.base.Preconditions.checkState;

import com.google.caliper.bridge.StartupAnnounceMessage;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.Maps;
import com.google.common.collect.MultimapBuilder;
import com.google.common.collect.SetMultimap;
import com.google.common.util.concurrent.AbstractExecutionThreadService;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.Service;
import com.google.common.util.concurrent.Service.State;
import com.google.common.util.concurrent.SettableFuture;
import com.google.gson.Gson;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import javax.annotation.concurrent.GuardedBy;

/**
 * A {@link Service} that manages a {@link ServerSocket}.
 *
 * <p> This service provides two pieces of functionality:
 * <ol>
 *   <li>It adapts {@link ServerSocket#accept()} to a {@link ListenableFuture} of an opened socket.
 *   <li>It demultiplexes incomming connections based on a {@link StartupAnnounceMessage} that is 
 *       sent over the socket.
 * </ol>
 *
 * <p>The {@linkplain State states} of this service are as follows:
 * <ul>
 *   <li>{@linkplain State#NEW NEW} : Idle state, the {@link ServerSocket} is not open yet.
 *   <li>{@linkplain State#STARTING STARTING} : {@link ServerSocket} is opened
 *   <li>{@linkplain State#RUNNING RUNNING} : We are continuously accepting and parsing connections
 *       from the socket.
 *   <li>{@linkplain State#STOPPING STOPPING} : The server socket is closing and all pending
 *       connection requests are terminated, connection requests will fail immediately.
 *   <li>{@linkplain State#TERMINATED TERMINATED} : Idle state, the socket is closed.
 *   <li>{@linkplain State#FAILED FAILED} : The service will transition to failed if it encounters
 *       any errors while accepting connections or reading from connections.
 * </ul>
 * 
 * <p>Note to future self.  There have been a few attempts to make it so that it is no longer 
 * necessary to dedicate a thread to this service (basically turn it into an AbstractIdleService).
 * The general idea has been to make callers to getConnection invoke accept, here is why it didn't
 * work.
 * <ul>
 *     <li>If you make getConnection a blocking method that calls accept until it finds the 
 *         connection with its id, then there is no way to deal with connections that never arrive.
 *         For example, if the worker crashes before connecting then the thread calling accept will
 *         block forever waiting for it.  The only way to unblock a thread stuck on accept() is to
 *         close the socket (this holds for ServerSocketChannels and normal ServerSockets), but we
 *         cannot do that in this case because the socket is a shared resource.
 *     <li>If you make getConnection a non-blocking, polling based method then you expose yourself
 *         to potential deadlocks (due to missed signals) depending on what thread you poll from.
 *         If the polling thread is any of the threads that are involved with processing messages
 *         from the worker I believe there to be a deadlock risk.  Basically, if the worker sends
 *         messages over its output streams and then calls Socket.connect, and no printing to stdout
 *         or stderr occurs while connecting. Then if the runner polls, but misses the connection
 *         and then tries to read again, it will deadlock.
 * </ul>
 */
@Singleton class ServerSocketService extends AbstractExecutionThreadService {
  private enum Source { REQUEST, ACCEPT}
  
  private final Lock lock = new ReentrantLock();
  
  /**
   * Contains futures that have either only been accepted or requested.  Once both occur they are
   * removed from this map.
   */
  @GuardedBy("lock")
  private final Map<UUID, SettableFuture<OpenedSocket>> halfFinishedConnections = Maps.newHashMap();
  
  /**
   * Contains the history of connections so we can ensure that each id is only accepted once and
   * requested once.
   */
  @GuardedBy("lock")
  private final SetMultimap<Source, UUID> connectionState =
      MultimapBuilder.enumKeys(Source.class).hashSetValues().build();
  
  private ServerSocket serverSocket;
  private final Gson gson;

  @Inject ServerSocketService(Gson gson) {
    this.gson = gson;
  }

  int getPort() {
    awaitRunning();
    checkState(serverSocket != null, "Socket has not been opened yet");
    return serverSocket.getLocalPort();
  }

  /**
   * Returns a {@link ListenableFuture} for an open connection corresponding to the given id.
   *
   * <p>N.B. calling this method 'consumes' the connection and as such calling it twice with the
   * same id will not work, the second future returned will never complete.  Similarly calling it
   * with an id that does not correspond to a worker trying to connect will also fail.
   */
  public ListenableFuture<OpenedSocket> getConnection(UUID id) {
    checkState(isRunning(), "You can only get connections from a running service: %s", this);
    return getConnectionImpl(id, Source.REQUEST);
  }

  @Override protected void startUp() throws Exception {
    serverSocket = new ServerSocket(0 /* bind to any available port */);
  }

  @Override protected void run() throws Exception {
    while (isRunning()) {
      Socket socket;
      try {
        socket = serverSocket.accept();
      } catch (SocketException e) {
        // we were closed
        return;
      }
      OpenedSocket openedSocket = OpenedSocket.fromSocket(socket);
      UUID id = gson.fromJson(openedSocket.reader().readLine(), StartupAnnounceMessage.class)
          .trialId();
      // N.B. you should not call set with the lock held, to prevent same thread executors from
      // running with the lock.
      getConnectionImpl(id, Source.ACCEPT).set(openedSocket);
    }
  }

  /**
   * Returns a {@link SettableFuture} from the map of connections.
   * 
   * <p>This method has the following properties:
   * <ul>
   *    <li>If the id is present in {@link #connectionState}, this will throw an 
   *        {@link IllegalStateException}.
   *    <li>The id and source are recorded in {@link #connectionState}
   *    <li>If the future is already in {@link #halfFinishedConnections}, it is removed and 
   *        returned.
   *    <li>If the future is not in {@link #halfFinishedConnections}, a new {@link SettableFuture} 
   *        is added and then returned.
   * 
   * <p>These features together ensure that each connection can only be accepted once, only 
   * requested once and once both have happened it will be removed from 
   * {@link #halfFinishedConnections}.
   */
  private SettableFuture<OpenedSocket> getConnectionImpl(UUID id, Source source) {
    lock.lock();
    try {
      checkState(connectionState.put(source, id), "Connection for %s has already been %s",
          id, source);
      SettableFuture<OpenedSocket> future = halfFinishedConnections.get(id);
      if (future == null) {
        future = SettableFuture.create();
        halfFinishedConnections.put(id, future);
      } else {
        halfFinishedConnections.remove(id);
      }
      return future;
    } finally {
      lock.unlock();
    }
  }

  @Override protected void triggerShutdown() {
    try {
      serverSocket.close();
    } catch (IOException e) {
      // best effort...
    }
  }

  @Override protected void shutDown() throws Exception {
    serverSocket.close();
    // Now we have either been asked to stop or have failed with some kind of exception, we want to
    // notify all pending requests, so if there are any references outside of this class they will
    // notice.
    lock.lock();
    try {
      for (SettableFuture<OpenedSocket> future : halfFinishedConnections.values()) {
        future.setException(new Exception("The socket has been closed"));
      }
      halfFinishedConnections.clear();
      connectionState.clear();
    } finally {
      lock.unlock();
    }
  }

  /**
   * A simple tuple for the opened streams of a socket.
   */
  static final class OpenedSocket {
    @VisibleForTesting static OpenedSocket fromSocket(Socket socket) throws IOException {
      // See comment in WorkerModule for why this is necessary.
      socket.setTcpNoDelay(true);
      return new OpenedSocket(
          new OutputStreamWriter(getOutputStream(socket), UTF_8), 
          new BufferedReader(new InputStreamReader(getInputStream(socket), UTF_8)));
    }
    
    private final BufferedReader reader;
    private final Writer writer;

    private OpenedSocket(Writer writer, BufferedReader reader) {
      this.reader = reader;
      this.writer = writer;
    }

    BufferedReader reader() {
      return reader;
    }
    
    Writer writer() {
      return writer;
    }
  }

  /**
   * Returns an {@link OutputStream} for the socket, but unlike {@link Socket#getOutputStream()}
   * when you call {@link OutputStream#close() close} it only closes the output end of the socket
   * instead of the entire socket.
   */
  private static OutputStream getOutputStream(final Socket socket) throws IOException {
    final OutputStream delegate = socket.getOutputStream();
    return new OutputStream() {

      @Override public void close() throws IOException {
        delegate.flush();
        synchronized (socket) {
          socket.shutdownOutput();
          if (socket.isInputShutdown()) {
            socket.close();
          }
        }
      }
    
      // Boring delegates from here on down
      @Override public void write(int b) throws IOException {
        delegate.write(b);
      }
    
      @Override public void write(byte[] b) throws IOException {
        delegate.write(b);
      }
    
      @Override public void write(byte[] b, int off, int len) throws IOException {
        delegate.write(b, off, len);
      }
    
      @Override public void flush() throws IOException {
        delegate.flush();
      }
    };
  }
  
  /**
   * Returns an {@link InputStream} for the socket, but unlike {@link Socket#getInputStream()}
   * when you call {@link InputStream#close() close} it only closes the input end of the socket
   * instead of the entire socket.
   */
  private static InputStream getInputStream(final Socket socket) throws IOException {
    final InputStream delegate = socket.getInputStream();
    return new InputStream() {
      @Override public void close() throws IOException {
        synchronized (socket) {
          socket.shutdownInput();
          if (socket.isOutputShutdown()) {
            socket.close();
          }
        }
      }
      
      // Boring delegates from here on down
      @Override public int read() throws IOException {
        return delegate.read();
      }
      
      @Override public int read(byte[] b) throws IOException {
        return delegate.read(b);
      }
      
      @Override public int read(byte[] b, int off, int len) throws IOException {
        return delegate.read(b, off, len);
      }
      
      @Override public long skip(long n) throws IOException {
        return delegate.skip(n);
      }
      
      @Override public int available() throws IOException {
        return delegate.available();
      }
      
      @Override public void mark(int readlimit) {
        delegate.mark(readlimit);
      }
      
      @Override public void reset() throws IOException {
        delegate.reset();
      }
      
      @Override public boolean markSupported() {
        return delegate.markSupported();
      }
    };
  }
}
