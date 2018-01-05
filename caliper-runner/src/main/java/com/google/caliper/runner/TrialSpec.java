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

package com.google.caliper.runner;

import com.google.caliper.bridge.CommandLineSerializer;
import com.google.caliper.bridge.ExperimentSpec;
import com.google.caliper.bridge.TrialRequest;
import com.google.caliper.bridge.WorkerRequest;
import com.google.caliper.model.BenchmarkSpec;
import com.google.caliper.runner.Instrument.InstrumentedMethod;
import com.google.caliper.runner.config.VmConfig;
import com.google.common.collect.ImmutableList;
import java.io.PrintWriter;
import java.util.UUID;
import javax.inject.Inject;

/**
 * A {@link WorkerSpec} for running a trial of an experiment.
 *
 * @author Colin Decker
 */
@TrialScoped
final class TrialSpec extends WorkerSpec {

  private final Experiment experiment;
  private final BenchmarkSpec benchmarkSpec;
  private final BenchmarkClass benchmarkClass;
  private final int trialNumber;
  private final int port;

  @Inject
  TrialSpec(
      @TrialId UUID id,
      Experiment experiment,
      BenchmarkSpec benchmarkSpec,
      BenchmarkClass benchmarkClass,
      @TrialNumber int trialNumber,
      @LocalPort int port) {
    super(id);
    this.experiment = experiment;
    this.benchmarkSpec = benchmarkSpec;
    this.benchmarkClass = benchmarkClass;
    this.trialNumber = trialNumber;
    this.port = port;
  }

  @Override
  public String name() {
    return "trial-" + trialNumber;
  }

  @Override
  public ImmutableList<String> args() {
    InstrumentedMethod instrumentedMethod = experiment.instrumentedMethod();
    WorkerRequest request =
        new TrialRequest(
            id(),
            port,
            new ExperimentSpec(
                experiment.id(),
                instrumentedMethod.type(),
                instrumentedMethod.workerOptions(),
                benchmarkSpec,
                ImmutableList.copyOf(instrumentedMethod.benchmarkMethod().getParameterTypes())));
    return ImmutableList.of(CommandLineSerializer.render(request));
  }

  /**
   * Returns a list of VM option flags that should be used when starting the worker VM.
   *
   * <p>These will be added to the command line in addition to options specified in the VM
   * configuration.
   */
  @Override
  public ImmutableList<String> vmOptions(VmConfig vmConfig) {
    Instrument instrument = experiment.instrumentedMethod().instrument();
    return new ImmutableList.Builder<String>()
        .addAll(benchmarkClass.vmOptions())
        .addAll(vmConfig.commonInstrumentVmArgs())
        .addAll(instrument.getExtraCommandLineArgs(vmConfig))
        .build();
  }

  @Override
  public void printInfoHeader(PrintWriter writer) {
    writer.println("Trial Number: " + trialNumber);
    writer.println("Trial Id: " + id());
    writer.println("Experiment: " + experiment);
  }
}
