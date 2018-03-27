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

import com.google.caliper.runner.config.VmConfig;
import com.google.caliper.runner.config.VmType;
import com.google.common.base.Optional;
import com.google.common.base.Predicate;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;

/** Abstraction of a VM of a specific type with a specific configuration. */
public abstract class Vm {

  private final VmConfig config;
  private final String classpath;

  Vm(VmConfig config, String classpath) {
    this.config = checkNotNull(config);
    this.classpath = checkNotNull(classpath);
  }

  /** Returns the configuration for this VM. */
  public VmConfig config() {
    return config;
  }

  /** Returns the name of this VM. */
  public final String name() {
    return config.name();
  }

  /** Returns the type of this VM. */
  public abstract VmType type();

  /** Returns the (optional) configured home directory path for this VM. */
  public final Optional<String> home() {
    return config.home();
  }

  /** Returns the name of or relative path to the VM executable file. */
  public abstract String executable();

  /**
   * Returns the full set of VM args to use for this VM, including any of the given additional args.
   *
   * <p>Note that the {@link #trialArgs()} will not be included unless explicitly passed to this
   * method.
   */
  public final ImmutableList<String> args(Iterable<String>... additionalArgs) {
    ImmutableList.Builder<String> builder = ImmutableList.builder();
    builder.addAll(config.args());
    for (Iterable<String> args : additionalArgs) {
      builder.addAll(args);
    }
    builder.addAll(classpathArgs());
    builder.addAll(lastArgs());
    return builder.build();
  }

  /**
   * Returns the set of VM args that should be used when starting a VM to run a benchmark trial.
   *
   * <p>These args may include things like tuning parameters or args to make the VM output
   * information for the instruments to parse and use.
   */
  // TODO(cgdecker): Should this go in TrialSpec?
  // But in TrialSpec it would need to be in the form "if VM is JVM ... else ...", which isn't great
  public abstract ImmutableSet<String> trialArgs();

  /** Returns the classpath that should be used for workers on this VM. */
  public final String classpath() {
    return classpath;
  }

  /** Returns the VM arguments to use for specifying the given classpath for the VM. */
  // NOTE: This mainly just exists because app_process is weird; all other supported VM executables
  // can just use "-cp <classpath>"
  protected abstract ImmutableList<String> classpathArgs();

  /**
   * Returns additional VM arguments that passed last after all other VM args (but before the main
   * class and its arguments).
   */
  // NOTE: This mainly just exists because app_process is weird and requires a specific arg just
  // before the main class.
  protected abstract ImmutableList<String> lastArgs();

  /**
   * Returns a predicate that selects the names of properties that should be included in the {@code
   * VmSpec} for a run.
   */
  public abstract Predicate<String> vmPropertiesToRetain();
}
