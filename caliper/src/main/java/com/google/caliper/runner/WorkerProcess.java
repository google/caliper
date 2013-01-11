/*
 * Copyright (C) 2012 Google Inc.
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

package com.google.caliper.runner;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.util.concurrent.ThreadFactoryBuilder;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.concurrent.ThreadFactory;

/**
 * A processor subclass that is specifically designed to be a worker process.  Thus, it will be
 * {@linkplain Process#destroy() destroyed} when the parent process exits.
 */
final class WorkerProcess extends Process {
  private static final ThreadFactory shutdownHookThreadFactory = new ThreadFactoryBuilder()
      .setNameFormat("worker-shutdown-hook-%d")
      .build();

  private static final ShutdownHookRegistrar runtimeRegistrar = new ShutdownHookRegistrar() {
    @Override
    public boolean removeShutdownHook(Thread hook) {
      return Runtime.getRuntime().removeShutdownHook(hook);
    }

    @Override
    public void addShutdownHook(Thread hook) {
      Runtime.getRuntime().addShutdownHook(hook);
    }
  };

  private final ShutdownHookRegistrar shutdownHookRegistrar;
  private final Process delegate;
  private final Thread shutdownHook;

  WorkerProcess(ProcessBuilder processBuilder) throws IOException {
    this(runtimeRegistrar, processBuilder.start());
  }

  @VisibleForTesting WorkerProcess(ShutdownHookRegistrar shutdownHookRegistrar, Process process) {
    this.shutdownHookRegistrar = shutdownHookRegistrar;
    this.delegate = process;
    this.shutdownHook = shutdownHookThreadFactory.newThread(new ProcessDestroyer(delegate));
    shutdownHookRegistrar.addShutdownHook(shutdownHook);
  }

  @Override
  public OutputStream getOutputStream() {
    return delegate.getOutputStream();
  }

  @Override
  public InputStream getInputStream() {
    return delegate.getInputStream();
  }

  @Override
  public InputStream getErrorStream() {
    return delegate.getErrorStream();
  }

  @Override
  public int waitFor() throws InterruptedException {
    int waitFor = delegate.waitFor();
    shutdownHookRegistrar.removeShutdownHook(shutdownHook);
    return waitFor;
  }

  @Override
  public int exitValue() {
    int exitValue = delegate.exitValue();
    // if it hasn't thrown, the process is done
    shutdownHookRegistrar.removeShutdownHook(shutdownHook);
    return exitValue;
  }

  @Override
  public void destroy() {
    delegate.destroy();
    shutdownHookRegistrar.removeShutdownHook(shutdownHook);
  }

  @VisibleForTesting interface ShutdownHookRegistrar {
    void addShutdownHook(Thread hook);
    boolean removeShutdownHook(Thread hook);
  }

  private static final class ProcessDestroyer implements Runnable {
    final Process process;

    ProcessDestroyer(Process process) {
      this.process = process;
    }

    @Override public void run() {
      process.destroy();
    }
  }
}
