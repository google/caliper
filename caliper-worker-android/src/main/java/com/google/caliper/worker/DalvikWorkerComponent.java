/*
 * Copyright (C) 2015 Google Inc.
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

package com.google.caliper.worker;

import com.google.caliper.bridge.BridgeModule;
import com.google.caliper.bridge.ExperimentSpec;
import com.google.caliper.runner.BenchmarkClassModule;
import dagger.BindsInstance;
import dagger.Component;
import javax.inject.Singleton;

/** Actual {@code WorkerComponent} for use on Android. */
@Singleton
@Component(modules = {BenchmarkClassModule.class, BridgeModule.class, WorkerModule.class})
interface DalvikWorkerComponent extends WorkerComponent {

  /** Builder for a {@code DalvikWorkerComponent}. */
  @Component.Builder
  interface Builder {
    /** Binds the {@code ExperimentSpec} for the worker. */
    @BindsInstance
    Builder experiment(ExperimentSpec experiment);

    /** Builds a new {@code DalvikWorkerComponent} instance. */
    DalvikWorkerComponent build();
  }
}
