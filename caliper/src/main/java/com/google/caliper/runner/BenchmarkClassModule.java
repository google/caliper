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

import com.google.caliper.runner.Running.AfterExperimentMethods;
import com.google.caliper.runner.Running.BeforeExperimentMethods;
import com.google.common.collect.ImmutableSet;
import dagger.Module;
import dagger.Provides;

import java.lang.reflect.Method;
import javax.inject.Singleton;

/**
 * Binds objects related to a benchmark class.
 */
// TODO(gak): move more of benchmark class into this module
@Module
public final class BenchmarkClassModule {

  @Provides
  @Singleton
  static BenchmarkClass provideBenchmarkClass(@Running.BenchmarkClass Class<?> benchmarkClassObject)
      throws InvalidBenchmarkException {
    return BenchmarkClass.forClass(benchmarkClassObject);
  }

  @Provides
  @BeforeExperimentMethods
  static ImmutableSet<Method> provideBeforeExperimentMethods(
      BenchmarkClass benchmarkClass) {
    return benchmarkClass.beforeExperimentMethods();
  }

  @Provides
  @AfterExperimentMethods
  static ImmutableSet<Method> provideAfterExperimentMethods(
      BenchmarkClass benchmarkClass) {
    return benchmarkClass.afterExperimentMethods();
  }
}
