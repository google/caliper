/**
 * Copyright (C) 2009 Google Inc.
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

import com.google.caliper.api.ResultProcessor;
import com.google.caliper.model.BenchmarkSpec;
import com.google.caliper.model.InstrumentSpec;
import com.google.caliper.model.Scenario;
import com.google.caliper.model.Trial;
import com.google.caliper.model.VmSpec;
import com.google.common.collect.Sets;
import com.google.inject.Inject;

import java.util.Set;

/**
 * Prints a brief summary of the results collected.  It does not contain the measurements themselves
 * as that is the responsibility of the webapp.
 */
final class ConsoleResultProcessor implements ResultProcessor {
  private final ConsoleWriter console;

  private Set<InstrumentSpec> instrumentSpecs = Sets.newHashSet();
  private Set<VmSpec> vmSpecs = Sets.newHashSet();
  private Set<BenchmarkSpec> benchmarkSpecs = Sets.newHashSet();
  private int numMeasurements = 0;

  @Inject ConsoleResultProcessor(ConsoleWriter console) {
    this.console = console;
  }

  @Override public void processTrial(Trial trial) {
    instrumentSpecs.add(trial.instrumentSpec());
    Scenario scenario = trial.scenario();
    vmSpecs.add(scenario.vmSpec());
    benchmarkSpecs.add(scenario.benchmarkSpec());
    numMeasurements += trial.measurements().size();
  }

  @Override public void close() {
    console.printf("Collected %d measurements from:%n", numMeasurements);
    console.printf("  %d instrument(s)%n", instrumentSpecs.size());
    console.printf("  %d virtual machine(s)%n", vmSpecs.size());
    console.printf("  %d benchmark(s)%n", benchmarkSpecs.size());
    console.flush();
  }
}
