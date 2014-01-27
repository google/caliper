package com.google.caliper.runner;

import com.google.caliper.config.CaliperConfig;
import com.google.caliper.config.InvalidConfigurationException;
import com.google.common.base.Charsets;
import com.google.common.collect.ImmutableMap;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;

import javax.annotation.concurrent.GuardedBy;

/**
 * A collection of Simple java executables and a helper method for creating process builders for 
 * them.
 */
final class FakeWorkers {

  @GuardedBy("FakeWorkers.class")
  private static VirtualMachine jvm;

  /** 
   * Try to find the currently executing jvm binary, N.B. This isn't guaranteed to be cross 
   * platform.
   */
  private static synchronized void init() {
    if (jvm == null) {
      try {
        jvm = new VirtualMachine("default", 
            new CaliperConfig(ImmutableMap.<String, String>of()).getDefaultVmConfig());
      } catch (InvalidConfigurationException e) {
        throw new RuntimeException();
      }
    }
  }

  /** 
   * Returns a ProcessBuilder that attempts to invoke the given class as main in a JVM configured
   * with a classpath equivalent to the currently executing JVM.
   */
  static ProcessBuilder createProcessBuilder(Class<?> mainClass, String ...mainArgs) {
    init();
    List<String> args;
    try {
      args = WorkerProcess.getJvmArgs(jvm, BenchmarkClass.forClass(mainClass));
    } catch (InvalidBenchmarkException e) {
      throw new RuntimeException(e);
    }
    args.add(mainClass.getName());
    Collections.addAll(args, mainArgs);
    return new ProcessBuilder().command(args);
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
  
  /** 
   * A simple main method that exits immediately with the code provided by the first argument
   */
  static final class Exit {
    public static void main(String[] args) {
      System.exit(Integer.parseInt(args[0]));
    }
  }
  
  /** 
   * A simple main method that exits immediately with the code provided by the first argument
   */
  static final class CloseAndSleep {
    public static void main(String[] args) throws IOException, InterruptedException {
      System.err.close();
      System.in.close();
      System.out.close();
      new CountDownLatch(1).await();  // wait forever
    }
  }
  
  /** 
   * Prints alternating arguments to standard out and standard error.
   */
  static final class PrintClient {
    public static void main(String[] args)  {
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
  
  /** 
   * Connects to a socket on localhost on the port provided as the first argument and echos all 
   * data.
   * 
   * <p>Once the connection has been closed it prints the remaining args to stdout
   */
  static final class SocketEchoClient {
    public static void main(String[] args) throws UnknownHostException, IOException {
      int port = Integer.parseInt(args[0]);
      Socket socket = new Socket(InetAddress.getLocalHost(), port);
      socket.setTcpNoDelay(true);
      BufferedReader reader = 
          new BufferedReader(new InputStreamReader(socket.getInputStream(), Charsets.UTF_8));
      OutputStreamWriter writer = 
          new OutputStreamWriter(socket.getOutputStream(), Charsets.UTF_8);
      writer.write("start\n");
      writer.flush();
      String line;
      while ((line = reader.readLine()) != null) {
        writer.write(line + "\n");
        writer.flush();
      }
      socket.close();
      for (int i = 1; i < args.length; i++) {
        System.out.println(args[i]);
        System.out.flush();
      }
    }
  }
}
