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

package com.google.caliper.runner;

import com.google.caliper.bridge.LogMessage;
import com.google.caliper.bridge.LogMessageVisitor;
import com.google.caliper.bridge.OpenedSocket;
import com.google.caliper.core.BenchmarkClassModel;
import com.google.caliper.runner.config.CaliperConfig;
import com.google.caliper.runner.config.InvalidConfigurationException;
import com.google.caliper.runner.config.VmConfig;
import com.google.caliper.runner.platform.JvmPlatform;
import com.google.caliper.runner.platform.Platform;
import com.google.caliper.util.Util;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
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
final class FakeWorkers {

  @GuardedBy("FakeWorkers.class")
  private static Target target;

  /**
   * Try to find the currently executing jvm binary, N.B. This isn't guaranteed to be cross
   * platform.
   */
  private static synchronized Target init() {
    if (target == null) {
      try {
        Platform platform = new JvmPlatform();
        target =
            Target.create(
                "default",
                new CaliperConfig(ImmutableMap.<String, String>of()).getDefaultVmConfig(platform));
      } catch (InvalidConfigurationException e) {
        throw new RuntimeException();
      }
    }
    return target;
  }

  /**
   * Returns a {@code Command} that attempts to invoke the given class as main in a JVM configured
   * with a classpath equivalent to the currently executing JVM.
   */
  static Command createCommand(Class<?> mainClass, String... mainArgs) {
    Target target = init();
    VmConfig vm = target.vm();
    BenchmarkClassModel benchmarkClass = BenchmarkClassModel.create(mainClass);

    // Don't add all the VM options that a normal worker has because we don't want things like
    // printing flags for the fake workers.
    return Command.builder()
        .putAllEnvironmentVariables(target.platform().workerEnvironment())
        .addArgument(vm.vmExecutable().getAbsolutePath())
        .addArguments(benchmarkClass.vmOptions())
        .addArguments(vm.workerClassPathArgs())
        .addArgument(mainClass.getName())
        .addArguments(ImmutableList.copyOf(mainArgs))
        .build();
  }

  public static Target getTarget() {
    return init();
  }

  /**
   * A simple main method that will sleep for the number of milliseconds specified in the first
   * argument.
   */
  static final class Sleeper {
    public static void main(String[] args) throws NumberFormatException, InterruptedException {
      Thread.sleep(Long.parseLong(args[0]));
    }
  }

  /** A simple main method that exits immediately with the code provided by the first argument */
  static final class Exit {
    public static void main(String[] args) {
      System.exit(Integer.parseInt(args[0]));
    }
  }

  /** A simple main method that exits immediately with the code provided by the first argument */
  static final class CloseAndSleep {
    public static void main(String[] args) throws IOException, InterruptedException {
      System.err.close();
      System.in.close();
      System.out.close();
      new CountDownLatch(1).await(); // wait forever
    }
  }

  /** Prints alternating arguments to standard out and standard error. */
  static final class PrintClient {
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
  @VisibleForTesting
  static final class LoadBenchmarkClass {

    public static void main(String[] args) throws ClassNotFoundException {
      String benchmarkClassName = args[0];
      Util.loadClass(benchmarkClassName);
    }
  }

  static final class DummyLogMessage extends LogMessage implements Serializable {
    private final String content;

    DummyLogMessage(String content) {
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
  static final class SocketEchoClient {
    public static void main(String[] args) throws Exception {
      int port = Integer.parseInt(args[0]);
      OpenedSocket openedSocket =
          OpenedSocket.fromSocket(new Socket(InetAddress.getLocalHost(), port));
      OpenedSocket.Reader reader = openedSocket.reader();
      OpenedSocket.Writer writer = openedSocket.writer();
      writer.write(new DummyLogMessage("start"));
      writer.flush();
      Serializable obj;
      while ((obj = reader.read()) != null) {
        writer.write(obj);
        writer.flush();
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
