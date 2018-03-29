/*
 * Copyright (C) 2012 Google Inc.
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

import com.google.caliper.bridge.LogMessageParserModule;
import com.google.caliper.core.Running.BenchmarkClass;
import com.google.caliper.json.GsonModule;
import com.google.caliper.model.Run;
import com.google.caliper.runner.config.CaliperConfig;
import com.google.caliper.runner.config.CaliperConfigModule;
import com.google.caliper.runner.options.CaliperOptions;
import com.google.caliper.runner.options.OptionsModule;
import com.google.caliper.runner.server.ServerModule;
import com.google.caliper.runner.target.DeviceModule;
import com.google.caliper.runner.target.TargetModule;
import com.google.caliper.runner.worker.WorkerOutputModule;
import com.google.caliper.runner.worker.targetinfo.TargetInfoComponent;
import com.google.caliper.runner.worker.targetinfo.TargetInfoFactory;
import com.google.caliper.runner.worker.targetinfo.TargetInfoFromWorkerFactory;
import com.google.caliper.util.OutputModule;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.MoreExecutors;
import dagger.Binds;
import dagger.Module;
import dagger.Provides;
import java.util.UUID;
import java.util.concurrent.Executors;
import javax.inject.Singleton;
import org.joda.time.Instant;

/** A Dagger module that configures bindings common to all {@link CaliperRun} implementations. */
@Module(
  includes = {
    DeviceModule.class,
    LogMessageParserModule.class,
    CaliperConfigModule.class,
    GsonModule.class,
    OptionsModule.class,
    OutputModule.class,
    ServerModule.class,
    ServiceModule.class,
    TargetModule.class,
    WorkerOutputModule.class
  },
  subcomponents = {TargetInfoComponent.class, CaliperRunComponent.class}
)
abstract class CaliperRunnerModule {
  private CaliperRunnerModule() {}

  private static final String RUNNER_MAX_PARALLELISM_OPTION = "runner.maxParallelism";

  @Provides
  static Instant provideInstant() {
    return Instant.now();
  }

  @Provides
  static UUID provideUuid() {
    return UUID.randomUUID();
  }

  @Provides
  @Singleton
  static Run provideRun(UUID uuid, CaliperOptions caliperOptions, Instant startTime) {
    return new Run.Builder(uuid).label(caliperOptions.runName()).startTime(startTime).build();
  }

  @Provides
  @Singleton
  static ListeningExecutorService provideExecutorService(CaliperConfig config) {
    int poolSize = Integer.parseInt(config.properties().get(RUNNER_MAX_PARALLELISM_OPTION));
    return MoreExecutors.listeningDecorator(Executors.newFixedThreadPool(poolSize));
  }

  @Binds
  abstract TargetInfoFactory bindTargetInfoFactory(TargetInfoFromWorkerFactory factory);

  @Provides
  @BenchmarkClass
  static String provideBenchmarkClassName(CaliperOptions options) {
    return options.benchmarkClassName();
  }
}
