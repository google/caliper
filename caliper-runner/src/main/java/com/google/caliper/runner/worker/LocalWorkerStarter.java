/*
 * Copyright (C) 2017 Google Inc.
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

package com.google.caliper.runner.worker;

import com.google.common.annotations.VisibleForTesting;
import java.io.InputStream;
import java.util.UUID;
import javax.inject.Inject;

/**
 * Runs commands on the local machine.
 *
 * @author Colin Decker
 */
final class LocalWorkerStarter implements WorkerStarter {

  private final ShutdownHookRegistrar shutdownHookRegistrar;
  private final boolean redirectErrorStream;

  @Inject
  LocalWorkerStarter(ShutdownHookRegistrar shutdownHookRegistrar) {
    this(shutdownHookRegistrar, false);
  }

  @VisibleForTesting
  LocalWorkerStarter(ShutdownHookRegistrar shutdownHookRegistrar, boolean redirectErrorStream) {
    this.shutdownHookRegistrar = shutdownHookRegistrar;
    this.redirectErrorStream = redirectErrorStream;
  }

  @Override
  public WorkerProcess startWorker(UUID id, Command command) throws Exception {
    ProcessBuilder builder = new ProcessBuilder().redirectErrorStream(redirectErrorStream);
    builder.environment().putAll(command.environment());
    builder.command(command.arguments());
    return new LocalWorkerProcess(id, shutdownHookRegistrar, builder.start());
  }

  /** A worker process that running on the local machine. */
  private static final class LocalWorkerProcess extends WorkerProcess {

    private final Process process;

    LocalWorkerProcess(UUID id, ShutdownHookRegistrar shutdownHookRegistrar, Process process) {
      super(id, shutdownHookRegistrar);
      this.process = process;
    }

    @Override
    public InputStream stdout() {
      return process.getInputStream();
    }

    @Override
    public InputStream stderr() {
      return process.getErrorStream();
    }

    @Override
    public int doAwaitExit() throws InterruptedException {
      return process.waitFor();
    }

    @Override
    public void doKill() {
      process.destroy();
    }
  }
}
