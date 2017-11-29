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

package com.google.caliper.runner;

import static com.google.common.base.Preconditions.checkNotNull;

import java.io.InputStream;
import java.util.UUID;

/**
 * Simple representation of a worker process which may be running locally or remotely.
 *
 * <p>Automatically handles registration with the {@link ShutdownHookRegistrar} to ensure the
 * process is killed when Caliper is.
 *
 * @author Colin Decker
 */
abstract class WorkerProcess {

  private final UUID id;
  private final ShutdownHookRegistrar shutdownHookRegistrar;
  private final Thread shutdownHook;

  protected WorkerProcess(UUID id, ShutdownHookRegistrar shutdownHookRegistrar) {
    this.id = checkNotNull(id);
    this.shutdownHookRegistrar = checkNotNull(shutdownHookRegistrar);
    this.shutdownHook =
        new Thread("worker-shutdown-hook-" + id) {
          @Override
          public void run() {
            doKill();
          }
        };
    shutdownHookRegistrar.addShutdownHook(shutdownHook);
  }

  /** Returns the ID assigned to this worker process. */
  public final UUID id() {
    return id;
  }

  /** Returns the process' standard output stream. */
  public abstract InputStream stdout();

  /** Returns the process' standard error stream. */
  public abstract InputStream stderr();

  /**
   * Waits for the process to exit and returns its exit code. If the process has already exited,
   * just returns the exit code.
   */
  public final int awaitExit() throws InterruptedException {
    int result = doAwaitExit();
    shutdownHookRegistrar.removeShutdownHook(shutdownHook);
    return result;
  }

  /** Waits for the process to exit and returns its exit code. */
  protected abstract int doAwaitExit() throws InterruptedException;

  /** Attempts to kill the process. */
  public final void kill() {
    doKill();
    shutdownHookRegistrar.removeShutdownHook(shutdownHook);
  }

  /** Attempts to kill the process. */
  protected abstract void doKill();
}
