/*
 * Copyright (C) 2018 Google Inc.
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

import static java.util.concurrent.TimeUnit.MINUTES;

import com.google.caliper.bridge.AbstractLogMessageVisitor;
import com.google.caliper.bridge.BenchmarkModelLogMessage;
import com.google.caliper.bridge.BenchmarkModelRequest;
import com.google.caliper.bridge.LogMessage;
import com.google.caliper.bridge.LogMessageVisitor;
import com.google.caliper.bridge.WorkerRequest;
import com.google.caliper.model.BenchmarkClassModel;
import com.google.caliper.runner.config.VmConfig;
import com.google.caliper.runner.options.CaliperOptions;
import com.google.caliper.util.ShortDuration;
import com.google.common.collect.ImmutableList;
import dagger.Binds;
import dagger.BindsInstance;
import dagger.Module;
import dagger.Subcomponent;
import java.io.PrintWriter;
import java.util.UUID;
import javax.annotation.Nullable;
import javax.inject.Inject;

/**
 * Component for creating a {@link WorkerRunner} for getting the class model from a specific target.
 */
@WorkerScoped
@Subcomponent(modules = {BenchmarkModelComponent.BenchmarkModelModule.class, WorkerModule.class})
interface BenchmarkModelComponent {
  WorkerRunner<BenchmarkClassModel> workerRunner();

  /** Builder for the component. */
  @Subcomponent.Builder
  interface Builder {
    /** Binds the target to get the model from. */
    @BindsInstance
    Builder target(Target target);

    /** Builds a new component. */
    BenchmarkModelComponent build();
  }

  /** Module with bindings needed for getting a benchmark model from a worker. */
  @Module
  abstract static class BenchmarkModelModule {
    @Binds
    abstract WorkerProcessor<BenchmarkClassModel> bindWorkerProcessor(Processor processor);

    @Binds
    abstract WorkerSpec bindWorkerSpec(Spec spec);
  }

  /** A {@link WorkerSpec} for getting a benchmark class model from a target. */
  @WorkerScoped
  static final class Spec extends WorkerSpec {

    private final Target target;
    private final CaliperOptions options;

    @Inject
    Spec(Target target, CaliperOptions options) {
      super(UUID.randomUUID());
      this.target = target;
      this.options = options;
    }

    @Override
    public String name() {
      return "benchmark-model-" + target.name();
    }

    @Override
    public WorkerRequest request() {
      return BenchmarkModelRequest.create(options.benchmarkClassName(), options.userParameters());
    }

    @Override
    public ImmutableList<String> vmOptions(VmConfig vmConfig) {
      // Use a relatively low heap size since nothing the worker does should require much memory.
      return ImmutableList.of("-Xms256m", "-Xmx1g");
    }

    @Override
    public void printInfoHeader(PrintWriter writer) {
      writer.println("Worker Id: " + id());
      writer.println("Benchmark Class Name: " + options.benchmarkClassName());
    }
  }

  /** {@link WorkerProcessor} for receiving a {@link BenchmarkClassModel} from the worker. */
  static final class Processor extends WorkerProcessor<BenchmarkClassModel> {

    @Nullable private volatile BenchmarkClassModel result = null;

    private final LogMessageVisitor successVisitor =
        new AbstractLogMessageVisitor() {
          @Override
          public void visit(BenchmarkModelLogMessage logMessage) {
            Processor.this.result = logMessage.model();
          }
        };

    @Inject
    Processor() {}

    @Override
    public ShortDuration timeLimit() {
      // Should not take nearly this long.
      return ShortDuration.of(5, MINUTES);
    }

    @Override
    public boolean handleMessage(LogMessage message, Worker worker) {
      message.accept(FailureLogMessageVisitor.INSTANCE);
      message.accept(successVisitor);
      return result != null;
    }

    @Override
    public BenchmarkClassModel getResult() {
      return result;
    }
  }
}
