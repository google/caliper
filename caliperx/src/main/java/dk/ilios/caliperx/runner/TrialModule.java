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

package dk.ilios.caliperx.runner;

import static com.google.common.base.Preconditions.checkState;

import dk.ilios.caliperx.bridge.OpenedSocket;
import dk.ilios.caliperx.model.BenchmarkSpec;
import dk.ilios.caliperx.model.Host;
import dk.ilios.caliperx.model.Run;
import dk.ilios.caliperx.model.Scenario;
import dk.ilios.caliperx.model.Trial;
import dk.ilios.caliperx.runner.Instrument.MeasurementCollectingVisitor;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.inject.AbstractModule;
import com.google.inject.Provides;

import java.util.UUID;

/**
 * Configuration for a {@link TrialRunLoop}.
 */
final class TrialModule extends AbstractModule {
  @Override protected void configure() {
    install(TrialScopes.module());
  }

  @TrialScoped
  @Provides
  BenchmarkSpec provideBenchmarkSpec(Experiment experiment) {
    return new BenchmarkSpec.Builder()
        .className(experiment.instrumentation().benchmarkMethod().getDeclaringClass().getName())
        .methodName(experiment.instrumentation().benchmarkMethod().getName())
        .addAllParameters(experiment.userParameters())
        .build();
  }

  @Provides
  @TrialScoped
  ListenableFuture<OpenedSocket> provideTrialSocket(
      @TrialId UUID trialId,
      ServerSocketService serverSocketService) {
    return serverSocketService.getConnection(trialId);
  }

  @Provides
  MeasurementCollectingVisitor provideMeasurementCollectingVisitor(Experiment experiment) {
    return experiment.instrumentation().getMeasurementCollectingVisitor();
  }

  @Provides
  @TrialScoped
  TrialSchedulingPolicy provideTrialSchedulingPolicy(Experiment experiment) {
    return experiment.instrumentation().instrument().schedulingPolicy();
  }

  @Provides TrialResultFactory provideTrialFactory(@TrialId final UUID trialId,
      final Run run,
      final Host host,
      final Experiment experiment,
      final BenchmarkSpec benchmarkSpec) {
    return new TrialResultFactory() {
      @Override public TrialResult newTrialResult(
          VmDataCollectingVisitor dataCollectingVisitor,
          MeasurementCollectingVisitor measurementCollectingVisitor) {
        checkState(measurementCollectingVisitor.isDoneCollecting());
        // TODO(lukes): should the trial messages be part of the Trial datastructure?  It seems like
        // the web UI could make use of them.
        return new TrialResult(
            new Trial.Builder(trialId)
                .run(run)
                .instrumentSpec(experiment.instrumentation().instrument().getSpec())
                .scenario(new Scenario.Builder()
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
