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

import com.google.caliper.bridge.OpenedSocket;
import com.google.caliper.bridge.TrialRequest;
import com.google.caliper.bridge.WorkerRequest;
import com.google.caliper.model.BenchmarkSpec;
import com.google.caliper.model.Host;
import com.google.caliper.model.Run;
import com.google.caliper.model.Scenario;
import com.google.caliper.model.Trial;
import com.google.caliper.runner.Instrument.Instrumentation;
import com.google.caliper.runner.Instrument.MeasurementCollectingVisitor;
import com.google.common.collect.ImmutableList;
import com.google.common.util.concurrent.ListenableFuture;
import dagger.Module;
import dagger.Provides;
import java.util.UUID;

/** Configuration for a {@link TrialRunLoop}. */
@Module
final class TrialModule {

  // TODO(cgdecker): Consider splitting this into two modules.
  // One with the fields that need to be provided in the constructor, one abstract class for better
  // performance.

  private final UUID trialId;
  private final int trialNumber;
  private final Experiment experiment;

  TrialModule(UUID trialId, int trialNumber, Experiment experiment) {
    this.trialId = trialId;
    this.trialNumber = trialNumber;
    this.experiment = experiment;
  }

  @TrialScoped
  @Provides
  @TrialId
  UUID provideTrialId() {
    return trialId;
  }

  @TrialScoped
  @Provides
  @TrialNumber
  int provideTrialNumber() {
    return trialNumber;
  }

  @TrialScoped
  @Provides
  Experiment provideExperiment() {
    return experiment;
  }

  @TrialScoped
  @Provides
  static BenchmarkSpec provideBenchmarkSpec(Experiment experiment) {
    return new BenchmarkSpec.Builder()
        .className(experiment.instrumentation().benchmarkMethod().getDeclaringClass().getName())
        .methodName(experiment.instrumentation().benchmarkMethod().getName())
        .addAllParameters(experiment.userParameters())
        .build();
  }

  @Provides
  @TrialScoped
  static WorkerRequest provideRequest(
      @TrialId UUID trialId,
      Experiment experiment,
      BenchmarkSpec benchmarkSpec,
      @LocalPort int port) {
    Instrumentation instrumentation = experiment.instrumentation();
    return new TrialRequest(
        trialId,
        instrumentation.type(),
        instrumentation.workerOptions(),
        benchmarkSpec,
        ImmutableList.copyOf(instrumentation.benchmarkMethod.getParameterTypes()),
        port);
  }

  @Provides
  @TrialScoped
  static ListenableFuture<OpenedSocket> provideTrialSocket(
      @TrialId UUID trialId, ServerSocketService serverSocketService) {
    return serverSocketService.getConnection(trialId);
  }

  @Provides
  static MeasurementCollectingVisitor provideMeasurementCollectingVisitor(Experiment experiment) {
    return experiment.instrumentation().getMeasurementCollectingVisitor();
  }

  @Provides
  @TrialScoped
  static TrialSchedulingPolicy provideTrialSchedulingPolicy(Experiment experiment) {
    return experiment.instrumentation().instrument().schedulingPolicy();
  }

  // TODO(user): make this a singleton in a higher level module.
  @Provides
  @TrialScoped
  static ShutdownHookRegistrar provideShutdownHook() {
    return new RuntimeShutdownHookRegistrar();
  }

  // TODO(cgdecker): This will need to be bound based on the device in the future.
  @Provides
  static WorkerStarter provideWorkerStarter(LocalWorkerStarter workerStarter) {
    return workerStarter;
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
                .instrumentSpec(experiment.instrumentation().instrument().getSpec())
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

  @Provides
  @TrialScoped
  static Command provideTrialCommand(
      WorkerCommandFactory commandFactory,
      Experiment experiment,
      BenchmarkClass benchmarkClass,
      WorkerRequest request) {
    return commandFactory.buildCommand(experiment, benchmarkClass, request);
  }
}
