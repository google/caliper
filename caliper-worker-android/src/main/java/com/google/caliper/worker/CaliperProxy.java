/*
 * Copyright (C) 2018 Google Inc.
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

package com.google.caliper.worker;

import static com.google.caliper.worker.CaliperProxyActivity.TAG;

import android.util.Log;
import com.google.caliper.bridge.FailureLogMessage;
import com.google.caliper.bridge.KillVmRequest;
import com.google.caliper.bridge.RemoteClasspathMessage;
import com.google.caliper.bridge.StartVmRequest;
import com.google.caliper.bridge.StopProxyRequest;
import com.google.caliper.bridge.VmStoppedMessage;
import com.google.caliper.util.Uuids;
import com.google.caliper.worker.connection.ClientConnectionService;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.io.ByteStreams;
import com.google.common.io.Closer;
import com.google.common.util.concurrent.AbstractExecutionThreadService;
import com.google.common.util.concurrent.MoreExecutors;
import com.google.common.util.concurrent.Service.Listener;
import com.google.common.util.concurrent.Service.State;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.nio.channels.Channels;
import java.nio.channels.SocketChannel;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import javax.inject.Inject;
import javax.inject.Singleton;

/**
 * Proxy for the runner that handles starting up worker VMs for it and connecting those VMs back to
 * the runner.
 */
@Singleton
final class CaliperProxy extends AbstractExecutionThreadService {

  private final InetSocketAddress clientAddress;
  private final ClientConnectionService clientConnection;
  private final ExecutorService executor;

  /**
   * The local classpath on this device that should be used for workers. See {@link
   * RemoteClasspathMessage}.
   */
  private final String classpath;

  /** Environment variables to add to the environment of VM processes we start. */
  private final ImmutableMap<String, String> processEnv;

  private final ConcurrentMap<UUID, ProcessHolder> processes = new ConcurrentHashMap<>();

  @Inject
  CaliperProxy(
      InetSocketAddress clientAddress,
      ClientConnectionService clientConnection,
      ExecutorService executor,
      String classpath,
      ImmutableMap<String, String> processEnv) {
    this.clientAddress = clientAddress;
    this.clientConnection = clientConnection;
    this.executor = executor;
    this.classpath = classpath;
    this.processEnv = processEnv;
  }

  @Override
  public void startUp() throws Exception {
    clientConnection.startAsync();
    addListener(
        new Listener() {
          @Override
          public void failed(State from, Throwable e) {
            notifyError(e);
          }
        },
        MoreExecutors.directExecutor());
    clientConnection.awaitRunning();
  }

  @Override
  public void run() throws Exception {
    clientConnection.send(RemoteClasspathMessage.create(classpath));

    while (isRunning()) {
      final Object request = clientConnection.receive();
      if (request == null || request instanceof StopProxyRequest) {
        return;
      }

      if (request instanceof StartVmRequest) {
        executor.execute(
            new Runnable() {
              @Override
              public void run() {
                try {
                  startVm((StartVmRequest) request);
                } catch (Throwable e) {
                  notifyError(e);
                }
              }
            });
      } else if (request instanceof KillVmRequest) {
        executor.execute(
            new Runnable() {
              @Override
              public void run() {
                try {
                  killVm(((KillVmRequest) request).vmId());
                } catch (Throwable e) {
                  notifyError(e);
                }
              }
            });
      }
    }
  }

  @Override
  public void shutDown() throws Exception {
    try {
      for (UUID vmId : processes.keySet()) {
        killVm(vmId);
      }
      for (ProcessHolder holder : processes.values()) {
        holder.awaitCompletion();
      }
    } finally {
      executor.shutdown();
      clientConnection.stopAsync();
    }
  }

  private void startVm(final StartVmRequest request) throws IOException {
    ProcessBuilder builder = new ProcessBuilder().command(request.command());
    builder.environment().putAll(processEnv);

    Process process = builder.start();

    UUID vmId = request.vmId();

    // Need threads for each of these things since there's sadly no non-blocking way of doing them.
    ImmutableList<Future<?>> futures =
        ImmutableList.of(
            pipeProcessInputStream(request.stdoutId(), process.getInputStream()),
            pipeProcessInputStream(request.stderrId(), process.getErrorStream()),
            awaitExit(vmId, process));

    processes.put(vmId, new ProcessHolder(process, futures));
  }

  private void killVm(UUID vmId) {
    ProcessHolder holder = processes.get(vmId);
    if (holder != null) {
      // Assuming the process is actually killed, this should lead to the thread waiting for the
      // proccess to exit to see it exit and the things that need to happen when it exits should
      // happen there.
      holder.kill();
    }
  }

  /**
   * Opens a socket connection using the given ID and then copies the given {@code InputStream} to
   * it, effectively piping the output from the process to the other end of the connection.
   */
  private Future<?> pipeProcessInputStream(UUID streamId, final InputStream in) {
    return executor.submit(
        new Runnable() {
          @Override
          public void run() {
            try {
              Closer closer = Closer.create();
              try {
                SocketChannel channel = closer.register(SocketChannel.open(clientAddress));
                Uuids.writeToChannel(streamId, channel);
                ByteStreams.copy(Channels.newChannel(in), channel);
              } catch (Throwable e) {
                throw closer.rethrow(e);
              } finally {
                closer.close();
              }
            } catch (IOException e) {
              notifyError(e);
            }
          }
        });
  }

  private Future<?> awaitExit(final UUID vmId, final Process process) {
    return executor.submit(
        new Runnable() {
          @Override
          public void run() {
            try {
              int exitCode = waitForUninterruptibly(process);
              if (clientConnection.isRunning()) {
                processes.remove(vmId);
                clientConnection.send(VmStoppedMessage.create(vmId, exitCode));
              }
            } catch (IOException e) {
              notifyError(e);
            }
          }
        });
  }

  private void notifyError(Throwable e) {
    Log.e(TAG, e.getMessage(), e);
    if (clientConnection.isRunning()) {
      try {
        clientConnection.send(FailureLogMessage.create(e));
      } catch (IOException ignore) {
      }
    }
  }

  private static int waitForUninterruptibly(Process process) {
    boolean interrupted = false;
    try {
      while (true) {
        try {
          return process.waitFor();
        } catch (InterruptedException e) {
          interrupted = true;
        }
      }
    } finally {
      if (interrupted) {
        Thread.currentThread().interrupt();
      }
    }
  }

  /** Holder for a process and futures representing threads associated with the process. */
  private static class ProcessHolder {
    private final Process process;
    private final ImmutableList<Future<?>> futures;

    ProcessHolder(Process process, ImmutableList<Future<?>> futures) {
      this.process = process;
      this.futures = futures;
    }

    void kill() {
      process.destroy();
    }

    void awaitCompletion() {
      for (Future<?> future : futures) {
        try {
          future.get();
        } catch (Exception ignore) {
        }
      }
    }
  }
}
