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

package com.google.caliper.util;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;

/**
 * A collection of utility methods for dealing with sockets.
 */
public final class Sockets {
  private Sockets() {}

  /**
   * Returns an {@link OutputStream} for the socket, but unlike {@link Socket#getOutputStream()}
   * when you call {@link OutputStream#close() close} it only closes the output end of the socket
   * instead of the entire socket.
   */
  public static OutputStream getOutputStream(final Socket socket) throws IOException {
    final OutputStream delegate = socket.getOutputStream();
    return new OutputStream() {

      @Override public void close() throws IOException {
        delegate.flush();
        socket.shutdownOutput();
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
  public static InputStream getInputStream(final Socket socket) throws IOException {
    final InputStream delegate = socket.getInputStream();
    return new InputStream() {
      @Override public void close() throws IOException {
        socket.shutdownInput();
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
