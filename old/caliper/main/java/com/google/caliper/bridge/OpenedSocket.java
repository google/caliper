/*
 * Copyright (C) 2014 Google Inc.
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

package com.google.caliper.bridge;

import static com.google.common.base.Preconditions.checkNotNull;

import java.io.Closeable;
import java.io.EOFException;
import java.io.Flushable;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.io.Serializable;
import java.net.Socket;
import java.nio.channels.SocketChannel;

/**
 * A simple tuple for the opened streams of a socket.
 */
public final class OpenedSocket {
  /**
   * Waits for the channel to connect and returns a new {@link OpenedSocket}.
   */
  public static OpenedSocket fromSocket(SocketChannel socket) throws IOException {
    socket.configureBlocking(true);
    socket.finishConnect();
    return fromSocket(socket.socket());
  }
  /**
   * Returns a new {@link OpenedSocket} for the given connected {@link Socket} instance.
   */
  public static OpenedSocket fromSocket(Socket socket) throws IOException {
    // Setting this to true disables Nagle's algorithm (RFC 896) which seeks to decrease packet
    // overhead by buffering writes while there are packets outstanding (i.e. haven't been ack'd).
    // This interacts poorly with another TCP feature called 'delayed acks' (RFC 1122) if the
    // application sends lots of small messages (which we do!).  Without this enabled messages sent
    // by the worker may be delayed by up to the delayed ack timeout (on linux this is 40-500ms,
    // though in practice I have only observed 40ms).  So we need to enable the TCP_NO_DELAY option
    // here.
    socket.setTcpNoDelay(true);
    // N.B. order is important here, constructing an ObjectOutputStream requires writing a header
    // and constructing an ObjectInputStream requires reading that header.  So we always need to
    // construct the OOS first so we don't deadlock. 
    ObjectOutputStream output = new ObjectOutputStream(getOutputStream(socket));
    ObjectInputStream input = new ObjectInputStream(getInputStream(socket));
    return new OpenedSocket(new Reader(input), new Writer(output));
  }
  
  private final Reader reader;
  private final Writer writer;

  private OpenedSocket(Reader reader,
      Writer objectOutputStream) {
    this.reader = reader;
    this.writer = objectOutputStream;
  }

  public Reader reader() {
    return reader;
  }
  
  public Writer writer() {
    return writer;
  }
  
  /** Reads objects from the socket. */
  public static final class Reader implements Closeable {
    private final ObjectInputStream input;

    Reader(ObjectInputStream is) {
      this.input = is;
    }
    
    /** Returns the next object, or {@code null} if we are at EOF. */
    public Serializable read() throws IOException {
      try {
        return (Serializable) checkNotNull(input.readObject());
      } catch (EOFException eof) {
        // TODO(lukes): The only 'better' way to handle this would be to use an explicit poison pill
        // marker in the stream.  Otherwise we just have to catch EOFException.
        return null;
      } catch (ClassNotFoundException e) {
        throw new AssertionError(e);
      }
    }

    @Override public void close() throws IOException  {
      input.close();
    }
  }

  /** Writes objects to the socket. */
  public static final class Writer implements Closeable, Flushable {
    private final ObjectOutputStream output;

    Writer(ObjectOutputStream output) {
      this.output = output;
    }

    /** Returns the next object, or {@code null} if we are at EOF. */
    public void write(Serializable serializable) throws IOException {
      output.writeObject(serializable);
    }

    @Override public void flush() throws IOException {
      output.flush();
    }

    @Override public void close() throws IOException {
      output.close();
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
