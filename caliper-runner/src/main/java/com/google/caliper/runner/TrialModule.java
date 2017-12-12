/*
 * Copyright (C) 2013 Google Inc.
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

import static com.google.common.base.Preconditions.checkState;

import com.google.caliper.model.BenchmarkSpec;
import com.google.caliper.model.Host;
import com.google.caliper.model.Run;
import com.google.caliper.model.Scenario;
import com.google.caliper.model.Trial;
import com.google.caliper.runner.Instrument.MeasurementCollectingVisitor;
import dagger.Module;
import dagger.Provides;
import java.util.UUID;

/** Configuration for a {@link TrialRunLoop}. */
@Module
final class TrialModule {

  // TODO(cgdecker): Consider splitting this into two modules.
  // One with the fields that need to be provided in the constructor, one abstract class for better
  // performance.

  private final int trialNumber;
  private final Experiment experiment;

  TrialModule(int trialNumber, Experiment experiment) {
    this.trialNumber = trialNumber;
    this.experiment = experiment;
  }

  @Provides
  @TrialScoped
  @TrialId
  UUID provideTrialId() {
    return UUID.randomUUID();
  }

  @Provides
  @TrialNumber
  int provideTrialNumber() {
    return trialNumber;
  }

  @Provides
  Experiment provideExperiment() {
    return experiment;
  }

  @Provides
  Target provideTarget(Experiment experiment) {
    return experiment.target();
  }

  @Provides
  WorkerSpec provideWorkerSpec(TrialSpec spec) {
    return spec;
  }

  @TrialScoped
  @Provides
  static BenchmarkSpec provideBenchmarkSpec(Experiment experiment) {
    return new BenchmarkSpec.Builder()
        .className(experiment.instrumentedMethod().benchmarkMethod().getDeclaringClass().getName())
        .methodName(experiment.instrumentedMethod().benchmarkMethod().getName())
        .addAllParameters(experiment.userParameters())
        .build();
  }

  @Provides
  static MeasurementCollectingVisitor provideMeasurementCollectingVisitor(Experiment experiment) {
    return experiment.instrumentedMethod().getMeasurementCollectingVisitor();
  }

  @Provides
  @TrialScoped
  static TrialSchedulingPolicy provideTrialSchedulingPolicy(Experiment experiment) {
    return experiment.instrumentedMethod().instrument().schedulingPolicy();
  }

  @Provides
  static TrialResultFactory provideTrialFactory(
      @TrialId final UUID trialId,
      final Run run,
      final Host host,
      final Experiment experiment,
      final BenchmarkSpec benchmarkSpec) {
    return new TrialResultFactory() {
      @Override
      public TrialResult newTrialResult(
          VmDataCollectingVisitor dataCollectingVisitor,
          MeasurementCollectingVisitor measurementCollectingVisitor) {
        checkState(measurementCollectingVisitor.isDoneCollecting());
        // TODO(lukes): should the trial messages be part of the Trial datastructure?  It seems like
        // the web UI could make use of them.
        return new TrialResult(
            new Trial.Builder(trialId)
                .run(run)
                .instrumentSpec(experiment.instrumentedMethod().instrument().getSpec())
                .scenario(
                    new Scenario.Builder()
                        .host(host)
                        .vmSpec(dataCollectingVisitor.vmSpec())
                        .benchmarkSpec(benchmarkSpec))
                .addAllMeasurements(measurementCollectingVisitor.getMeasurements())
                .build(),
            experiment,
            measurementCollectingVisitor.getMessages());
      }
    };
  }
}
