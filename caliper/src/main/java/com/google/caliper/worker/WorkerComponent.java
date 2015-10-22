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
import com.google.caliper.runner.BenchmarkClassModule;
import com.google.caliper.runner.ExperimentModule;
import dagger.Component;
import javax.inject.Singleton;

/**
 * Creates {@link Worker} for an {@link com.google.caliper.runner.Experiment}.
 */
@Singleton
@Component(modules = {
    BenchmarkClassModule.class,
    BridgeModule.class,
    ExperimentModule.class,
    WorkerModule.class
})
interface WorkerComponent {
  Worker getWorker();
}
