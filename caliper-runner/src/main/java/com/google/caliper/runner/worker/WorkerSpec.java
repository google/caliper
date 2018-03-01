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

import com.google.caliper.bridge.WorkerRequest;
import com.google.caliper.runner.config.VmConfig;
import com.google.common.collect.ImmutableList;
import java.io.PrintWriter;
import java.util.UUID;

/**
 * Spec for a worker to run, not including the target VM to run it on.
 *
 * @author Colin Decker
 */
public abstract class WorkerSpec {

  private final UUID id;

  protected WorkerSpec(UUID id) {
    this.id = id;
  }

  /**
   * Returns the ID of the worker.
   */
  public final UUID id() {
    return id;
  }

  /**
   * A unique (within a single Caliper run) name for this worker. Used for display to the user and
   * in naming the file that worker output is written to.
   */
  public String name() {
    return "worker-" + id;
  }

  /**
   * Returns the fully-qualified name of the worker class to run.
   */
  public String mainClass() {
    return "com.google.caliper.worker.WorkerMain";
  }

  /** Returns the request to send to the worker once it starts. */
  public abstract WorkerRequest request();

  /**
   * Returns a list of VM option flags that should be used when starting the worker VM.
   *
   * <p>These will be added to the command line in addition to options specified in the target VM
   * configuration.
   */
  public ImmutableList<String> vmOptions(VmConfig vm) {
    return ImmutableList.of();
  }

  /**
   * Prints a header describing this worker that goes at the top of the output file for the worker.
   */
  public void printInfoHeader(PrintWriter writer) {}
}
