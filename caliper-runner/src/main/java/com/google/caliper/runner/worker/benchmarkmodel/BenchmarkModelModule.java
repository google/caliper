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

package com.google.caliper.runner.worker.benchmarkmodel;

import com.google.caliper.core.BenchmarkClassModel;
import com.google.caliper.runner.worker.WorkerModule;
import com.google.caliper.runner.worker.WorkerProcessor;
import com.google.caliper.runner.worker.WorkerSpec;
import dagger.Binds;
import dagger.Module;

/** Module with bindings needed for getting a benchmark model from a worker. */
@Module(includes = WorkerModule.class)
abstract class BenchmarkModelModule {
  private BenchmarkModelModule() {}

  @Binds
  abstract WorkerProcessor<BenchmarkClassModel> bindWorkerProcessor(
      BenchmarkModelWorkerProcessor processor);

  @Binds
  abstract WorkerSpec bindWorkerSpec(BenchmarkModelWorkerSpec spec);
}
