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

package com.google.caliper.runner.worker.trial;

import com.google.caliper.bridge.TrialRequest;
import com.google.caliper.bridge.WorkerRequest;
import com.google.caliper.core.BenchmarkClassModel;
import com.google.caliper.runner.experiment.Experiment;
import com.google.caliper.runner.instrument.Instrument;
import com.google.caliper.runner.server.LocalPort;
import com.google.caliper.runner.worker.WorkerSpec;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableList;
import java.io.PrintWriter;
import java.util.UUID;
import javax.inject.Inject;

/** A {@link WorkerSpec} for running a trial of an experiment. */
public final class TrialSpec extends WorkerSpec {

  private final Experiment experiment;
  private final BenchmarkClassModel benchmarkClass;
  private final int trialNumber;

  @VisibleForTesting
  @Inject
  public TrialSpec(
      @TrialId UUID id,
      @LocalPort int port,
      Experiment experiment,
      BenchmarkClassModel benchmarkClass,
      @TrialNumber int trialNumber) {
    super(experiment.target(), id, id, port, experiment.benchmarkSpec().className());
    this.experiment = experiment;
    this.benchmarkClass = benchmarkClass;
    this.trialNumber = trialNumber;
  }

  @Override
  public String name() {
    return "trial-" + trialNumber;
  }

  @Override
  public WorkerRequest request() {
    return new TrialRequest(experiment.toExperimentSpec());
  }

  @Override
  public ImmutableList<String> additionalVmOptions() {
    Instrument instrument = experiment.instrumentedMethod().instrument();
    return new ImmutableList.Builder<String>()
        .addAll(benchmarkClass.vmOptions())
        .addAll(target().vm().trialArgs())
        .addAll(instrument.getExtraCommandLineArgs(target().vm().config()))
        .build();
  }

  @Override
  public void printInfoHeader(PrintWriter writer) {
    writer.println("Trial Number: " + trialNumber);
    writer.println("Trial Id: " + id());
    writer.println("Experiment: " + experiment);
  }
}
