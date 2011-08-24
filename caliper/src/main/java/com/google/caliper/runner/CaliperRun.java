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
import static java.util.concurrent.TimeUnit.MILLISECONDS;

import com.google.caliper.InterleavedReader;
import com.google.caliper.api.Benchmark;
import com.google.caliper.api.SkipThisScenarioException;
import com.google.caliper.model.CaliperData;
import com.google.caliper.util.InvalidCommandException;
import com.google.caliper.util.ShortDuration;
import com.google.caliper.util.Util;
import com.google.caliper.worker.WorkerMain;
import com.google.caliper.worker.WorkerRequest;
import com.google.caliper.worker.WorkerResponse;
import com.google.common.base.Charsets;
import com.google.common.base.Objects;
import com.google.common.base.Predicates;
import com.google.common.base.Stopwatch;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableSetMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.io.Closeables;
import com.google.gson.JsonObject;

import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.Collection;
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
  private final List<ResultProcessor> resultProcessors;

  public CaliperRun(CaliperOptions options, CaliperRc caliperRc, ConsoleWriter console)
      throws InvalidCommandException, InvalidBenchmarkException {
    this.options = options;
    this.caliperRc = caliperRc;
    this.console = console;

    Class<?> aClass = classForName(options.benchmarkClassName());
    this.benchmarkClass = new BenchmarkClass(aClass);
    this.instrument = Instrument.createInstrument(options.instrumentName(), caliperRc);
    this.methods = chooseBenchmarkMethods(benchmarkClass, instrument, options);
    this.resultProcessors = createResultProcessors();

    benchmarkClass.validateParameters(options.userParameters());
  }

  // TODO: this class does too much stuff. find some things to factor out of it.

  public void run() throws UserCodeException {
    ImmutableList<VirtualMachine> vms = createVms(options.vmNames());

    ImmutableSetMultimap<String, String> combinedParams =
        benchmarkClass.userParameters().fillInDefaultsFor(options.userParameters());

    ImmutableSetMultimap<String, String> vmArguments =
        benchmarkClass.injectableVmArguments().fillInDefaultsFor(options.vmArguments());

    // TODO(kevinb): other kinds of partial scenario selectors...
    ScenarioSelection selection = new FullCartesianScenarioSelection(
        methods, vms, combinedParams, vmArguments);

    console.describe(selection);

    ImmutableSet<Scenario> allScenarios = selection.buildScenarios();

    console.beforeDryRun(allScenarios.size());
    console.flush();

    // always dry run first.
    ImmutableSet<Scenario> scenariosToRun = dryRun(allScenarios);
    if (scenariosToRun.size() != allScenarios.size()) {
      console.skippedScenarios(allScenarios.size() - scenariosToRun.size());
    }

    ShortDuration estimate;
    try {
      ShortDuration perTrial = instrument.estimateRuntimePerTrial();
      estimate = perTrial.times(scenariosToRun.size() * options.trials());

    } catch (Exception e) {
      estimate = ShortDuration.zero();
    }

    console.beforeRun(options.trials(), scenariosToRun.size(), estimate);
    console.flush();

    if (options.dryRun()) {
      return;
    }

    ResultDataWriter results = new ResultDataWriter();
    results.writeInstrument(instrument);
    results.writeEnvironment(new EnvironmentGetter().getEnvironmentSnapshot());

    Stopwatch stopwatch = new Stopwatch().start();
    for (Scenario scenario : scenariosToRun) {
      TrialResult trialResult = measure(scenario);
      results.writeTrialResult(trialResult);
    }
    // TODO(kevinb): just use stopwatch.elapsed() after that's in Guava
    console.afterRun(ShortDuration.of(stopwatch.elapsedMillis(), MILLISECONDS));

    CaliperData caliperData = results.getData();
    for (ResultProcessor resultProcessor : resultProcessors) {
      resultProcessor.handleResults(caliperData);
    }
  }

  private ImmutableList<VirtualMachine> createVms(Set<String> vmNames) {
    ImmutableList.Builder<VirtualMachine> builder = ImmutableList.builder();
    if (vmNames.isEmpty()) {
      builder.add(VirtualMachine.hostVm());
    } else {
      for (String vmName : vmNames) {
        builder.add(findVm(vmName));
      }
    }
    return builder.build();
  }

  private VirtualMachine findVm(String vmName) {
    String home = Objects.firstNonNull(caliperRc.homeDirForVm(vmName), vmName);
    String absoluteHome =
        new File(home).isAbsolute() ? home : caliperRc.vmBaseDirectory() + "/" + home;
    List<String> verboseModeArgs = caliperRc.verboseArgsForVm(vmName);
    return VirtualMachine.from(
        vmName, absoluteHome, caliperRc.vmArgsForVm(vmName), verboseModeArgs);
  }

  private TrialResult measure(Scenario scenario) {
    WorkerRequest request = new WorkerRequest(
        instrument.workerOptions(),
        instrument.workerClass().getName(),
        benchmarkClass.name(),
        scenario.benchmarkMethod().name(),
        scenario.userParameters(),
        scenario.vmArguments());

    ProcessBuilder processBuilder = new ProcessBuilder().redirectErrorStream(true);

    List<String> args = processBuilder.command();

    args.add(scenario.vm().execPath.getAbsolutePath());
    args.addAll(scenario.vmArguments().values());

    addAll(args, "-cp", System.getProperty("java.class.path"));
    if (options.detailedLogging()) {
      args.addAll(scenario.vm().verboseModeArgs);
    }

    args.add(WorkerMain.class.getName());
    args.add(request.toString());

    Process process = null;
    try {
      process = processBuilder.start();
    } catch (IOException e) {
      throw new AssertionError(e); // ???
    }

    List<String> eventLog = Lists.newArrayList();
    Reader in = null;
    WorkerResponse response = null;

    try {
      in = new InputStreamReader(process.getInputStream(), Charsets.UTF_8);
      InterleavedReader reader = new InterleavedReader(in);
      Object o;
      while ((o = reader.read()) != null) {
        if (o instanceof String) {
          // TODO(schmoe): transform some of these messages, possibly with some configurability.
          // (especially for GC and JIT-compilation messages; also add timestamps)
          eventLog.add((String) o);
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
      // TODO(schmoe): This happens if the benchmark throws an exception. We should either make
      // this exception include the data sent to eventLog, or else make the calling code dump the
      // eventLog's contents on exception.
      throw new RuntimeException("Got no response!");
    }

    return new TrialResult(scenario, response.measurements, eventLog, args);
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

  private List<ResultProcessor> createResultProcessors() {
    // TODO(schmoe): add custom ResultProcessors via .caliperrc
    ImmutableList.Builder<ResultProcessor> builder = ImmutableList.builder();
    builder.add(new ConsoleResultProcessor(options.calculateAggregateScore()));
    builder.add(new OutputFileDumper(options.outputFileOrDir(), benchmarkClass.name()));
    return builder.build();
  }

  /**
   * Attempts to run each given scenario once, in the current VM. Returns a set of all of the
   * scenarios that didn't throw a {@link SkipThisScenarioException}.
   */
  ImmutableSet<Scenario> dryRun(Set<Scenario> scenarios) throws UserCodeException {
    ImmutableSet.Builder<Scenario> builder = ImmutableSet.builder();
    for (Scenario scenario : scenarios) {
      try {
        Benchmark benchmark = benchmarkClass.createAndStage(scenario);
        instrument.dryRun(benchmark, scenario.benchmarkMethod());
        builder.add(scenario);
        // discard 'benchmark' now; the worker will have to instantiate its own anyway
      } catch (SkipThisScenarioException innocuous) {
      }
    }
    return builder.build();
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
