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

package com.google.caliper.runner.worker.dryrun;

import com.google.caliper.bridge.DryRunRequest;
import com.google.caliper.bridge.ExperimentSpec;
import com.google.caliper.bridge.WorkerRequest;
import com.google.caliper.core.BenchmarkClassModel;
import com.google.caliper.runner.config.VmConfig;
import com.google.caliper.runner.experiment.Experiment;
import com.google.caliper.runner.target.Target;
import com.google.caliper.runner.worker.WorkerScoped;
import com.google.caliper.runner.worker.WorkerSpec;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import java.io.PrintWriter;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import javax.inject.Inject;

/**
 * A {@link WorkerSpec} for a dry-run of an experiment.
 *
 * @author Colin Decker
 */
@WorkerScoped
final class DryRunSpec extends WorkerSpec {

  private final BenchmarkClassModel benchmarkClass;
  private final ImmutableSet<Experiment> experiments;
  private final Target target;

  @Inject
  DryRunSpec(BenchmarkClassModel benchmarkClass, Set<Experiment> experiments, Target target) {
    super(UUID.randomUUID());
    this.benchmarkClass = benchmarkClass;
    this.experiments = ImmutableSet.copyOf(experiments);
    this.target = target;
  }

  @Override
  public String name() {
    return "dry-run-" + target.name();
  }

  @Override
  public WorkerRequest request() {
    Set<ExperimentSpec> experimentSpecs = new HashSet<>();
    for (Experiment experiment : experiments) {
      experimentSpecs.add(experiment.toExperimentSpec());
    }
    return new DryRunRequest(experimentSpecs);
  }

  @Override
  public ImmutableList<String> vmOptions(VmConfig vmConfig) {
    // For dry runs, don't add most extra VM config options. No measurements are taken in dry runs,
    // so things like configuration for the allocation instrument aren't needed. Do add options
    // specified by the benchmark class itself, which are typically things like -Xmx that may be
    // necessary for the benchmark to even run.
    return benchmarkClass.vmOptions().asList();
  }

  @Override
  public void printInfoHeader(PrintWriter writer) {
    writer.println("Worker Id: " + id());
    writer.println("Experiments:");
    for (Experiment experiment : experiments) {
      writer.println("  " + experiment);
    }
  }
}
