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

import com.google.caliper.api.Benchmark;
import com.google.caliper.api.SkipThisScenarioException;
import com.google.caliper.util.InvalidCommandException;
import com.google.caliper.util.SimpleDuration;
import com.google.caliper.util.Util;
import com.google.common.base.Predicates;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableSetMultimap;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

import java.util.Collection;
import java.util.Iterator;
import java.util.Set;

/**
 * A single execution of the benchmark runner, for a particular set of options.
 */
public final class CaliperRun {
  private final CaliperOptions options;
  private final CaliperRc caliperRc;
  private final ConsoleWriter console;
  private final BenchmarkClass benchmarkClass;
  private final Collection<BenchmarkMethod> methods;
  private final Instrument instrument;

  public CaliperRun(CaliperOptions options, CaliperRc caliperRc, ConsoleWriter console)
      throws InvalidCommandException, InvalidBenchmarkException {
    this.options = options;
    this.caliperRc = caliperRc;
    this.console = console;

    instrument = options.instrument();

    Class<?> aClass = classForName(options.benchmarkClassName());
    this.benchmarkClass = new BenchmarkClass(aClass);
    this.methods = chooseBenchmarkMethods();

    benchmarkClass.validateParameters(options.userParameters());
  }

  public void run() throws UserCodeException {
    ImmutableList<VirtualMachine> vms = options.vms();

    ImmutableSetMultimap<String, String> combinedParams =
        benchmarkClass.userParameters().fillInDefaultsFor(options.userParameters());

    ImmutableSetMultimap<String, String> vmArguments =
        benchmarkClass.injectableVmArguments().fillInDefaultsFor(options.vmArguments());

    // TODO(kevinb): other kinds of partial scenario selectors...
    ScenarioSelection selection = new FullCartesianScenarioSelection(
        methods, vms, combinedParams, vmArguments);

    console.describe(selection);

    Set<Scenario> mutableScenarios = Sets.newHashSet(selection.buildScenarios());

    console.beforeDryRun(mutableScenarios.size());
    console.flush();

    // always dry run first.
    dryRun(/*INOUT*/mutableScenarios);

    int finalScenarioCount = mutableScenarios.size();

    SimpleDuration estimate;
    try {
      SimpleDuration perTrial = instrument.estimateRuntimePerTrial();
      estimate = perTrial.times(finalScenarioCount * options.trials());

    } catch (Exception e) {
      estimate = SimpleDuration.ofNanos(0);
    }

    console.beforeRun(options.trials(), finalScenarioCount, estimate);
    console.flush();

    if (options.dryRun()) {
      return;
    }

    // TODO(kevinb): now the wet run!
  }

  private Collection<BenchmarkMethod> chooseBenchmarkMethods() throws InvalidBenchmarkException {
    ImmutableMap<String, BenchmarkMethod> methodMap =
        benchmarkClass.findAllBenchmarkMethods(instrument);

    ImmutableSet<String> names = options.benchmarkMethodNames();

    // TODO(kevinb): this doesn't seem to prevent bogus names on cmd line yet
    return names.isEmpty()
        ? methodMap.values()
        : Maps.filterKeys(methodMap, Predicates.in(names)).values();
  }

  void dryRun(Set<Scenario> mutableScenarios) throws UserCodeException {
    Iterator<Scenario> it = mutableScenarios.iterator();
    while (it.hasNext()) {
      Scenario scenario = it.next();
      try {
        Benchmark benchmark = benchmarkClass.createAndStage(scenario);
        instrument.dryRun(benchmark, scenario.benchmarkMethod());
      } catch (SkipThisScenarioException innocuous) {
        it.remove();
      }
    }
  }

  private static Class<?> classForName(String className)
      throws InvalidCommandException, InvalidBenchmarkException {
    try {
      return Util.lenientClassForName(className);
    } catch (ClassNotFoundException e) {
      throw new InvalidCommandException("Benchmark class not found: " + className);
    } catch (ExceptionInInitializerError e) {
      throw new UserCodeException(
          "Exception thrown while initializing class '" + className + "'", e.getCause());
    }
  }
}
