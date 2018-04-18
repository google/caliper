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

package com.google.caliper.runner.target;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.util.concurrent.Uninterruptibles.getUninterruptibly;

import com.google.caliper.bridge.KillVmRequest;
import com.google.caliper.bridge.OpenedSocket;
import com.google.caliper.bridge.RemoteClasspathMessage;
import com.google.caliper.bridge.StartVmRequest;
import com.google.caliper.bridge.StopProxyRequest;
import com.google.caliper.bridge.VmStoppedMessage;
import com.google.caliper.runner.server.ServerSocketService;
import com.google.common.io.Closer;
import com.google.common.util.concurrent.AbstractExecutionThreadService;
import com.google.common.util.concurrent.SettableFuture;
import java.io.IOException;
import java.io.InputStream;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutionException;
import javax.inject.Inject;

/**
 * Service for managing a connection to a proxy for the Caliper runner running on a remote device.
 */
final class ProxyConnectionService extends AbstractExecutionThreadService {

  private final UUID proxyId = UUID.randomUUID();

  private final ServerSocketService server;

  private final Closer closer = Closer.create();
  private OpenedSocket.Reader reader;
  private OpenedSocket.Writer writer;

  private final SettableFuture<String> classpathFuture = SettableFuture.create();
  private final ConcurrentMap<UUID, VmProxy> vms = new ConcurrentHashMap<>();

  @Inject
  ProxyConnectionService(ServerSocketService server) {
    this.server = server;
  }

  /** Returns the random UUID that identifies this proxy connection. */
  public UUID proxyId() {
    return proxyId;
  }

  @Override
  public void startUp() throws IOException, ExecutionException, InterruptedException {
    // Some other class should have handled starting the proxy process, so just wait for it to
    // open its connection to us.
    OpenedSocket socket = server.getConnection(proxyId).get();
    this.reader = closer.register(socket.reader());
    this.writer = closer.register(socket.writer());
  }

  @Override
  public void run() throws IOException {
    while (isRunning()) {
      Object message = reader.read();
      if (message == null) {
        return;
      }

      if (message instanceof VmStoppedMessage) {
        VmStoppedMessage stoppedMessage = (VmStoppedMessage) message;
        UUID vmId = stoppedMessage.vmId();
        VmProxy vm = vms.remove(vmId);
        if (vm != null) {
          vm.stopped(stoppedMessage.exitCode());
        }
      } else if (message instanceof RemoteClasspathMessage) {
        classpathFuture.set(((RemoteClasspathMessage) message).classpath());
      }
    }

    writer.write(new StopProxyRequest());
  }

  /** Returns the classpath to use for processes on the remote device. */
  public String getRemoteClasspath() throws ExecutionException {
    return getUninterruptibly(classpathFuture);
  }

  /**
   * Starts a VM by sending the given request and return a {@link VmProcess} instance representing
   * that VM.
   */
  public VmProcess startVm(StartVmRequest request) throws ExecutionException, IOException {
    // Create the proxy for the VM first so that it's already in the map so there's no possibility
    // of a race with the process exiting and sending a VmStoppedMessage back.
    VmProxy vm = new VmProxy(request.vmId(), request.stdoutId(), request.stderrId());
    vms.put(request.vmId(), vm);

    // Send the request to actually start the VM.
    writer.write(request);

    vm.awaitStarted();
    return vm;
  }

  @Override
  public void shutDown() throws IOException {
    closer.close();
  }

  /** Proxy for a VM process running on the remote device. */
  private final class VmProxy extends VmProcess {
    private final UUID vmId;
    private final UUID stdoutId;
    private final UUID stderrId;

    private volatile InputStream stdout;
    private volatile InputStream stderr;

    private final SettableFuture<Integer> exitCode = SettableFuture.create();

    VmProxy(UUID vmId, UUID stdoutId, UUID stderrId) {
      this.vmId = checkNotNull(vmId);
      this.stdoutId = checkNotNull(stdoutId);
      this.stderrId = checkNotNull(stderrId);
    }

    /** Waits for the stdout/stderr connections to be established. */
    void awaitStarted() throws ExecutionException {
      this.stdout = getUninterruptibly(server.getInputStream(stdoutId));
      this.stderr = getUninterruptibly(server.getInputStream(stderrId));
    }

    /** Sets the exit code of the process, unblocking {@code awaitExit()}. */
    void stopped(int exitCode) {
      this.exitCode.set(exitCode);
    }

    @Override
    public InputStream stdout() {
      return stdout;
    }

    @Override
    public InputStream stderr() {
      return stderr;
    }

    @Override
    protected int doAwaitExit() throws InterruptedException {
      try {
        return exitCode.get();
      } catch (ExecutionException e) {
        throw new RuntimeException(e);
      }
    }

    @Override
    protected void doKill() {
      try {
        writer.write(KillVmRequest.create(vmId));
      } catch (IOException e) {
        throw new RuntimeException(e);
      }
    }
  }
}
