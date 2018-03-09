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

package com.google.caliper.runner.worker.trial;

import static com.google.common.base.Preconditions.checkState;

import com.google.caliper.model.Host;
import com.google.caliper.model.Run;
import com.google.caliper.model.Scenario;
import com.google.caliper.model.Trial;
import com.google.caliper.runner.experiment.Experiment;
import com.google.caliper.runner.instrument.MeasurementCollectingVisitor;
import com.google.caliper.runner.target.Target;
import com.google.caliper.runner.worker.WorkerModule;
import com.google.caliper.runner.worker.WorkerProcessor;
import com.google.caliper.runner.worker.WorkerRunner;
import com.google.caliper.runner.worker.WorkerScoped;
import com.google.caliper.runner.worker.WorkerSpec;
import dagger.Binds;
import dagger.Provides;
import dagger.producers.ProducerModule;
import dagger.producers.Produces;
import java.util.UUID;

/** Configuration for running a trial. */
@ProducerModule(includes = WorkerModule.class)
abstract class TrialModule {

  @Produces
  static TrialResult trialResult(WorkerRunner<TrialResult> trialResultWorkerRunner) {
    return trialResultWorkerRunner.runWorker();
  }

  @Binds
  abstract WorkerProcessor<TrialResult> bindTrialProcessor(TrialProcessor processor);

  @Provides
  @WorkerScoped
  @TrialId
  static UUID provideTrialId() {
    return UUID.randomUUID();
  }

  @Provides
  static Target provideTarget(Experiment experiment) {
    return experiment.target();
  }

  @Binds
  abstract WorkerSpec bindWorkerSpec(TrialSpec spec);

  @Provides
  static MeasurementCollectingVisitor provideMeasurementCollectingVisitor(Experiment experiment) {
    return experiment.instrumentedMethod().getMeasurementCollectingVisitor();
  }

  @Provides
  static TrialResultFactory provideTrialFactory(
      @TrialId final UUID trialId, final Run run, final Host host, final Experiment experiment) {
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
                        .benchmarkSpec(experiment.benchmarkSpec()))
                .addAllMeasurements(measurementCollectingVisitor.getMeasurements())
                .build(),
            experiment,
            measurementCollectingVisitor.getMessages());
      }
    };
  }
}
