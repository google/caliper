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

import static java.util.Collections.addAll;

import com.google.caliper.InterleavedReader;
import com.google.caliper.api.Benchmark;
import com.google.caliper.api.SkipThisScenarioException;
import com.google.caliper.util.InvalidCommandException;
import com.google.caliper.util.ShortDuration;
import com.google.caliper.util.Util;
import com.google.caliper.worker.MicrobenchmarkWorker;
import com.google.caliper.worker.WorkerMain;
import com.google.caliper.worker.WorkerRequest;
import com.google.caliper.worker.WorkerResponse;
import com.google.common.base.Charsets;
import com.google.common.base.Joiner;
import com.google.common.base.Predicates;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableSetMultimap;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.google.common.io.Closeables;
import com.google.gson.JsonObject;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
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

    Class<?> aClass = classForName(options.benchmarkClassName());
    this.benchmarkClass = new BenchmarkClass(aClass);
    this.instrument = Instrument.createInstrument(options.instrumentName(), caliperRc);
    this.methods = chooseBenchmarkMethods(benchmarkClass, instrument, options);

    benchmarkClass.validateParameters(options.userParameters());
  }

  // TODO: this class does too much stuff. find some things to factor out of it.

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

    ShortDuration estimate;
    try {
      ShortDuration perTrial = instrument.estimateRuntimePerTrial();
      estimate = perTrial.times(finalScenarioCount * options.trials());

    } catch (Exception e) {
      estimate = ShortDuration.zero();
    }

    console.beforeRun(options.trials(), finalScenarioCount, estimate);
    console.flush();

    if (options.dryRun()) {
      return;
    }

    ResultDataWriter results = new ResultDataWriter();

    for (Scenario scenario : mutableScenarios) {
      measure(scenario);
    }
  }

  private void measure(Scenario scenario) {
    WorkerRequest request = new WorkerRequest(
        instrument.workerOptions(),
        MicrobenchmarkWorker.class.getName(),
        benchmarkClass.name(),
        scenario.benchmarkMethod().name(),
        scenario.userParameters());

    ProcessBuilder processBuilder = new ProcessBuilder().redirectErrorStream(true);

    List<String> args = processBuilder.command();

    args.add(scenario.vm().execPath.getAbsolutePath());

    // args.addAll(vmArgs); // TODO!

    addAll(args, "-cp", System.getProperty("java.class.path"));
    args.add(WorkerMain.class.getName());

    args.add(request.toString());

    Process process = null;

    System.out.println("Command: " + Joiner.on(' ').join(args));

    try {
      process = processBuilder.start();
    } catch (IOException e) {
      throw new AssertionError(e); // ???
    }

    StringBuilder eventLog = new StringBuilder(1000);
    Reader in = null;
    WorkerResponse response = null;

    try {
      in = new InputStreamReader(process.getInputStream(), Charsets.UTF_8);
      InterleavedReader reader = new InterleavedReader(in);
      Object o;
      while ((o = reader.read()) != null) {
        if (o instanceof String) {
          eventLog.append(o);
        } else {
          JsonObject jsonObject = (JsonObject) o;
          response = Util.GSON.fromJson(jsonObject, WorkerResponse.class);
        }
      }
    } catch (IOException e) {
      throw new AssertionError(e); // possible?

    } finally {
      Closeables.closeQuietly(in);
      process.destroy();
    }

    if (response == null) {
      throw new RuntimeException("Got no response!"); // TODO: ?
    }

    System.out.println("I got this: " + response.toString());
    System.out.println();

    System.out.println("Full event log: \n\n" + eventLog);
  }


  private static Collection<BenchmarkMethod> chooseBenchmarkMethods(
      BenchmarkClass benchmarkClass, Instrument instrument, CaliperOptions options)
          throws InvalidBenchmarkException {
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
        // discard 'benchmark' now; the worker will have to instantiate its own anyway
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
