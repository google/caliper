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
  private final Instrument instrument;

  // the following (with the Host) create a Scenario
  private final BenchmarkMethod benchmarkMethod;
  private final VirtualMachine vm;
  private final ImmutableSortedMap<String, String> userParameters;

  Experiment(
      Instrument instrument,
      BenchmarkMethod benchmarkMethod,
      Map<String, String> userParameters,
      VirtualMachine vm) {
    this.instrument = instrument;
    this.benchmarkMethod = benchmarkMethod;
    this.userParameters = ImmutableSortedMap.copyOf(userParameters);
    this.vm = vm;
  }

  Instrument instrument() {
    return instrument;
  }

  BenchmarkMethod benchmarkMethod() {
    return benchmarkMethod;
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
      return this.instrument.equals(that.instrument)
          && this.benchmarkMethod.equals(that.benchmarkMethod)
          && this.vm.equals(that.vm)
          && this.userParameters.equals(that.userParameters);
    }
    return false;
  }

  @Override public int hashCode() {
    return Objects.hashCode(instrument, benchmarkMethod, vm, userParameters);
  }

  @Override public String toString() {
    return Objects.toStringHelper(this)
        .add("instrument", instrument)
        .add("benchmarkMethod", benchmarkMethod)
        .add("vm", vm)
        .add("userParameters", userParameters)
        .toString();
  }
}
