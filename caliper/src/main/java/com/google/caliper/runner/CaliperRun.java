/*
 * Copyright (C) 2011 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */

package com.google.caliper.runner;

import com.google.caliper.spi.Instrument;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Predicates;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableSetMultimap;
import com.google.common.collect.Maps;

import java.io.PrintWriter;
import java.util.Collection;

/**
 * A single execution of the benchmark runner, for a particular set of options.
 */
public final class CaliperRun {
  private final CaliperOptions options;
  private final PrintWriter writer;

  /**
   * Standard constructor.
   */
  public CaliperRun(CaliperOptions options, PrintWriter writer) {
    this.options = options;
    this.writer = writer;
  }

  public void execute() {
    BenchmarkClass benchmarkClass = options.benchmarkClass();
    Collection<BenchmarkMethod> methods = findBenchmarkMethods(options);

    ImmutableSetMultimap<String,String> userParameters =
        benchmarkClass.resolveUserParameters(options.userParameters());

    ImmutableList<VirtualMachine> vms = options.vms();

    ScenarioSet scenarios = new ScenarioSet(
        methods, vms, userParameters, options.vmArguments());

    if (options.dryRun()) {
      for (Scenario scenario : scenarios) {
        options.instrument().dryRun(scenario);
      }
    }
    displayEstimate(scenarios);
  }

  @VisibleForTesting
  static Collection<BenchmarkMethod> findBenchmarkMethods(CaliperOptions options) {
    Instrument instrument = options.instrument();
    ImmutableMap<String, BenchmarkMethod> methodMap =
        options.benchmarkClass().findAllBenchmarkMethods(instrument);

    ImmutableSet<String> names = options.benchmarkNames();
    return names.isEmpty()
        ? methodMap.values()
        : Maps.filterKeys(methodMap, Predicates.in(names)).values();
  }

  private void displayEstimate(ScenarioSet scenarios) {
    writer.format("Measuring %s trials each of %s scenarios.", options.trials(), scenarios.size());
    try {
      int estimate = options.instrument().estimateRuntimeSeconds(scenarios, options);
      writer.format(" Estimated runtime: %s minutes.%n", (estimate + 59) / 60);
    } catch (UnsupportedOperationException e) {
      writer.format(" (Cannot estimate runtime.)%n");
    }
  }
}
