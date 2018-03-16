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

import static com.google.common.base.Functions.toStringFunction;
import static com.google.common.base.Preconditions.checkNotNull;

import com.google.caliper.bridge.WorkerRequest;
import com.google.caliper.runner.config.VmConfig;
import com.google.caliper.runner.target.VmProcess;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableList;
import java.io.PrintWriter;
import java.util.UUID;

/**
 * Spec for a worker to run, not including the target VM to run it on.
 *
 * @author Colin Decker
 */
public abstract class WorkerSpec implements VmProcess.Spec {

  private final VmConfig vm;
  private final UUID id;
  private final ImmutableList<String> args;

  protected WorkerSpec(VmConfig vm, UUID id, Object... args) {
    this(vm, id, FluentIterable.from(args).transform(toStringFunction()));
  }

  protected WorkerSpec(VmConfig vm, UUID id, Iterable<String> args) {
    this.vm = checkNotNull(vm);
    this.id = checkNotNull(id);
    this.args = ImmutableList.copyOf(args);
  }

  @Override
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

  @Override
  public final VmConfig vm() {
    return vm;
  }

  @Override
  public final ImmutableList<String> vmOptions() {
    return new ImmutableList.Builder<String>()
        .addAll(vm.options())
        .addAll(additionalVmOptions())
        .addAll(vm.workerClassPathArgs())
        .addAll(vm.workerProcessArgs())
        .build();
  }

  /**
   * Returns a list of VM options to add to the command line in addition to those specified by
   * default for the VM configuration.
   */
  protected ImmutableList<String> additionalVmOptions() {
    return ImmutableList.of();
  }

  @Override
  public String mainClass() {
    return "com.google.caliper.worker.WorkerMain";
  }

  @Override
  public final ImmutableList<String> mainArgs() {
    return args;
  }

  /** Returns the request to send to the worker once it starts. */
  public abstract WorkerRequest request();

  /**
   * Prints a header describing this worker that goes at the top of the output file for the worker.
   */
  public void printInfoHeader(PrintWriter writer) {}
}
