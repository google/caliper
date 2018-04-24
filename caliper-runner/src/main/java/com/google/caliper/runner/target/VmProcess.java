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

package com.google.caliper.runner.target;

import com.google.common.collect.ImmutableList;
import java.io.InputStream;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * Simple representation of a VM process running on a device.
 *
 * @author Colin Decker
 */
public abstract class VmProcess {

  private final Set<StopListener> listeners = new HashSet<>();
  private boolean running = true;

  /** Adds the given {@link Listener} to be notified when this process stops. */
  public final synchronized void addStopListener(StopListener listener) {
    listeners.add(listener);
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
    stopped();
    return result;
  }

  /** Waits for the process to exit and returns its exit code. */
  protected abstract int doAwaitExit() throws InterruptedException;

  /** Attempts to kill the process. */
  public final void kill() {
    doKill();
  }

  /** Attempts to kill the process. */
  protected abstract void doKill();

  private synchronized void stopped() {
    if (running) {
      running = false;
      for (StopListener listener : listeners) {
        listener.stopped(this);
      }
    }
  }

  /** A listener for process stop. */
  public interface StopListener {
    /** Called when the given {@code process} stops. */
    void stopped(VmProcess process);
  }

  /** Specification for a VM process. */
  public interface Spec {
    /** Returns a unique ID associated with the process. */
    UUID id();

    /** Returns the target that should run the process. */
    Target target();

    /** Returns a list of VM options to use for the process. */
    ImmutableList<String> vmOptions();

    /** Returns the name of the main class for the process. */
    String mainClass();

    /** Returns a list of args to pass to the main class. */
    ImmutableList<String> mainArgs();
  }

  /** A logger for information on and output from a {@link VmProcess}. */
  public interface Logger {

    /** Logs a line of text. */
    void log(String line);

    /** Logs a line of text, prefixing it with the given source (e.g. "stderr"). */
    void log(String source, String line);
  }
}
