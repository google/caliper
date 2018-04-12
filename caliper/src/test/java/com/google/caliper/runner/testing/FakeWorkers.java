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

package com.google.caliper.runner.testing;

import com.google.caliper.bridge.LogMessage;
import com.google.caliper.bridge.LogMessageVisitor;
import com.google.caliper.bridge.OpenedSocket;
import com.google.caliper.runner.target.Device;
import com.google.caliper.runner.target.LocalDevice;
import com.google.caliper.runner.target.Target;
import com.google.caliper.util.Util;
import java.io.IOException;
import java.io.Serializable;
import java.net.InetAddress;
import java.net.Socket;
import java.util.concurrent.CountDownLatch;
import javax.annotation.concurrent.GuardedBy;

/**
 * A collection of Simple java executables and a helper method for creating process builders for
 * them.
 */
public final class FakeWorkers {

  @GuardedBy("FakeWorkers.class")
  private static Target target;

  /**
   * Try to find the currently executing jvm binary, N.B. This isn't guaranteed to be cross
   * platform.
   */
  static synchronized Target init() {
    if (target == null) {
      Device device = LocalDevice.builder().build();
      target = device.createDefaultTarget();
    }
    return target;
  }

  public static Target getTarget() {
    return init();
  }

  /**
   * A simple main method that will sleep for the number of milliseconds specified in the first
   * argument.
   */
  public static final class Sleeper {
    public static void main(String[] args) throws NumberFormatException, InterruptedException {
      Thread.sleep(Long.parseLong(args[0]));
    }
  }

  /** A simple main method that exits immediately with the code provided by the first argument */
  public static final class Exit {
    public static void main(String[] args) {
      System.exit(Integer.parseInt(args[0]));
    }
  }

  /** A simple main method that exits immediately with the code provided by the first argument */
  public static final class CloseAndSleep {
    public static void main(String[] args) throws IOException, InterruptedException {
      System.err.close();
      System.in.close();
      System.out.close();
      new CountDownLatch(1).await(); // wait forever
    }
  }

  /** Prints alternating arguments to standard out and standard error. */
  public static final class PrintClient {
    public static void main(String[] args) {
      for (int i = 0; i < args.length; i++) {
        if (i % 2 == 0) {
          System.out.println(args[i]);
          System.out.flush();
        } else {
          System.err.println(args[i]);
          System.err.flush();
        }
      }
    }
  }

  /** Prints alternating arguments to standard out and standard error. */
  public static final class LoadBenchmarkClass {

    public static void main(String[] args) throws ClassNotFoundException {
      String benchmarkClassName = args[0];
      Util.loadClass(benchmarkClassName);
    }
  }

  public static final class DummyLogMessage extends LogMessage implements Serializable {
    private final String content;

    public DummyLogMessage(String content) {
      this.content = content;
    }

    @Override
    public void accept(LogMessageVisitor visitor) {}

    @Override
    public String toString() {
      return content;
    }

    @Override
    public boolean equals(Object obj) {
      return obj instanceof DummyLogMessage && ((DummyLogMessage) obj).content.equals(content);
    }

    @Override
    public int hashCode() {
      return content.hashCode();
    }
  }

  /**
   * Connects to a socket on localhost on the port provided as the first argument and echos all
   * data.
   *
   * <p>Once the connection has been closed it prints the remaining args to stdout
   */
  public static final class SocketEchoClient {
    public static void main(String[] args) throws Exception {
      int port = Integer.parseInt(args[0]);
      OpenedSocket openedSocket =
          OpenedSocket.fromSocket(new Socket(InetAddress.getLocalHost(), port));
      OpenedSocket.Reader reader = openedSocket.reader();
      OpenedSocket.Writer writer = openedSocket.writer();
      writer.write(new DummyLogMessage("start"));
      Serializable obj;
      while ((obj = reader.read()) != null) {
        writer.write(obj);
      }
      writer.close();
      reader.close();
      for (int i = 1; i < args.length; i++) {
        System.out.println(args[i]);
        System.out.flush();
      }
    }
  }
}
