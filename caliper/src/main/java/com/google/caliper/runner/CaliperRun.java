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
import com.google.caliper.util.Util;
import com.google.common.base.Predicates;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableSetMultimap;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;
import com.google.common.collect.Ordering;
import com.google.common.collect.Sets;

import java.io.File;
import java.io.PrintWriter;
import java.text.ParseException;
import java.util.Collection;
import java.util.Iterator;
import java.util.Set;

/**
 * A single execution of the benchmark runner, for a particular set of options.
 */
public final class CaliperRun {
  private final CaliperRc caliperRc;
  private final CaliperOptions options;
  private final PrintWriter writer;
  private final BenchmarkClass benchmarkClass;
  private final ImmutableSetMultimap<String, Object> userParameters;

  /**
   * Standard constructor.
   */
  public CaliperRun(PrintWriter writer, File rcFile, String[] args)
      throws InvalidCommandException, InvalidBenchmarkException {
    this.writer = writer;
    this.caliperRc = CaliperRcManager.loadOrCreate(rcFile);
    this.options = ParsedOptions.from(args, caliperRc);

    Class<?> aClass = classForName(options.benchmarkClassName());
    this.benchmarkClass = new BenchmarkClass(aClass);
    this.userParameters = convertUserParameters(options.userParameters());
  }

  // Simply converts all the strings to the right datatypes
  private ImmutableSetMultimap<String, Object> convertUserParameters(
      Multimap<String, String> rawValues) throws InvalidCommandException {
    ImmutableSetMultimap.Builder<String, Object> builder = ImmutableSetMultimap.builder();
    builder.orderKeysBy(Ordering.natural());

    for (String paramName : rawValues.keySet()) {
      Parameter p = benchmarkClass.userParameters().get(paramName);
      if (p == null) {
        throw new InvalidCommandException("unrecognized parameter: " + paramName);
      }

      for (String s : rawValues.get(paramName)) {
        try {
          builder.put(paramName, p.parser().parse(s));
        } catch (ParseException e) {
          throw new InvalidCommandException("Wrong type for " + paramName); // TODO(kevinb): better
        }
      }
    }
    return builder.build();
  }

  public void execute() throws InvalidBenchmarkException {
    // Arbitrary division right now between what's done in constructor and what's done here.

    Collection<BenchmarkMethod> methods = findBenchmarkMethods(benchmarkClass, options);

    ImmutableList<VirtualMachine> vms = options.vms();

    ImmutableSetMultimap<String, Object> combinedParams =
        benchmarkClass.userParameters().fillInDefaultsFor(userParameters);

    ImmutableSetMultimap<String, String> vmArguments =
        benchmarkClass.injectableVmArguments().fillInDefaultsFor(options.vmArguments());

    // TODO(kevinb): other kinds of partial scenario selectors...
    ScenarioSelection selection = new FullCartesianScenarioSelection(
        methods, vms, combinedParams, vmArguments);

    Set<Scenario> mutableScenarios = Sets.newHashSet(selection.buildScenarios());

    // always dry run first.
    dryRun(/*INOUT*/mutableScenarios);
    displayEstimate(mutableScenarios.size());

    if (options.dryRun()) {
      return;
    }

    // TODO(kevinb): now the wet run!
  }

  public static Collection<BenchmarkMethod> findBenchmarkMethods(
      BenchmarkClass benchmarkClass, CaliperOptions options)
      throws InvalidBenchmarkException {
    Instrument instrument = options.instrument();
    ImmutableMap<String, BenchmarkMethod> methodMap =
        benchmarkClass.findAllBenchmarkMethods(instrument);

    ImmutableSet<String> names = options.benchmarkMethodNames();
    return names.isEmpty()
        ? methodMap.values()
        : Maps.filterKeys(methodMap, Predicates.in(names)).values(); // TODO
  }

  public void dryRun(Set<Scenario> mutableScenarios)
      throws InvalidBenchmarkException {
    Instrument instrument = options.instrument();

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

  public void displayEstimate(int scenarioCount) {
    writer.format("Measuring %s trials each of %s scenarios.", options.trials(), scenarioCount);
    try {
      int estimate = options.instrument().estimateRuntimeSeconds(scenarioCount, options);
      writer.format(" Estimated runtime: %s minutes.%n", (estimate + 59) / 60);
    } catch (UnsupportedOperationException e) {
      writer.format(" (Cannot estimate runtime.)%n");
    }
  }

  private static Class<?> classForName(String className)
      throws InvalidCommandException, InvalidBenchmarkException {
    try {
      return Util.lenientClassForName(className);
    } catch (ClassNotFoundException e) {
      throw new InvalidCommandException("Benchmark class not found: " + className);
    } catch (ExceptionInInitializerError e) {
      throw new UserCodeException(e.getCause());
    }
  }
}
