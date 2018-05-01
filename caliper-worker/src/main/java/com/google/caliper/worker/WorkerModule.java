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

package com.google.caliper.worker;

import com.google.caliper.AfterExperiment;
import com.google.caliper.BeforeExperiment;
import com.google.caliper.core.Running.AfterExperimentMethods;
import com.google.caliper.core.Running.BeforeExperimentMethods;
import com.google.caliper.core.Running.BenchmarkClass;
import com.google.caliper.util.Reflection;
import com.google.caliper.worker.handler.RequestHandlerModule;
import com.google.common.collect.ImmutableSet;
import dagger.Module;
import dagger.Provides;
import dagger.Reusable;
import java.lang.reflect.Method;
import java.util.Random;
import javax.inject.Singleton;

/** Module providing bindings needed by the {@link Worker}. */
@Module(includes = {WorkerOptionsModule.class, RequestHandlerModule.class})
abstract class WorkerModule {
  private WorkerModule() {}

  @Provides
  @Singleton
  static Random provideRandom() {
    return new Random();
  }

  @Provides
  @Reusable
  @BeforeExperimentMethods
  static ImmutableSet<Method> provideBeforeExperimentMethods(
      @BenchmarkClass Class<?> benchmarkClass) {
    return Reflection.getAnnotatedMethods(benchmarkClass, BeforeExperiment.class);
  }

  @Provides
  @Reusable
  @AfterExperimentMethods
  static ImmutableSet<Method> provideAfterExperimentMethods(
      @BenchmarkClass Class<?> benchmarkClass) {
    return Reflection.getAnnotatedMethods(benchmarkClass, AfterExperiment.class);
  }
}
