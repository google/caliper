/*
 * Copyright (C) 2011 Google Inc.
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

import com.google.caliper.runner.Instrument.Instrumentation;
import com.google.common.base.MoreObjects;
import com.google.common.base.Objects;
import com.google.common.collect.ImmutableSortedMap;

import java.util.Map;

/**
 * A single "premise" for making benchmark measurements: which class and method to invoke, which VM
 * to use, which choices for user parameters and vmArguments to fill in and which instrument to use
 * to measure. A caliper run will compute all possible scenarios using
 * {@link FullCartesianExperimentSelector}, and will run one or more trials of each.
 */
final class Experiment {
  private final Instrumentation instrumentation;
  private final VirtualMachine vm;
  private final ImmutableSortedMap<String, String> userParameters;

  Experiment(
      Instrumentation instrumentation,
      Map<String, String> userParameters,
      VirtualMachine vm) {
    this.instrumentation = checkNotNull(instrumentation);
    this.userParameters = ImmutableSortedMap.copyOf(userParameters);
    this.vm = checkNotNull(vm);
  }

  Instrumentation instrumentation() {
    return instrumentation;
  }

  ImmutableSortedMap<String, String> userParameters() {
    return userParameters;
  }

  VirtualMachine vm() {
    return vm;
  }

  @Override public boolean equals(Object object) {
    if (object instanceof Experiment) {
      Experiment that = (Experiment) object;
      return this.instrumentation.equals(that.instrumentation)
          && this.vm.equals(that.vm)
          && this.userParameters.equals(that.userParameters);
    }
    return false;
  }

  @Override public int hashCode() {
    return Objects.hashCode(instrumentation, vm, userParameters);
  }

  @Override public String toString() {
    return MoreObjects.toStringHelper("")
        .add("instrument", instrumentation.instrument())
        .add("benchmarkMethod", instrumentation.benchmarkMethod.getName())
        .add("vm", vm.name)
        .add("parameters", userParameters)
        .toString();
  }
}
