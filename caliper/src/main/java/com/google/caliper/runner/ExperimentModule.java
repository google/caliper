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

import static com.google.caliper.runner.Running.Benchmark;
import static com.google.caliper.runner.Running.BenchmarkMethod;
import static com.google.common.base.Preconditions.checkNotNull;

import com.google.caliper.bridge.WorkerSpec;
import com.google.caliper.util.Util;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSortedMap;
import dagger.Module;
import dagger.Provides;

import java.lang.reflect.Method;

/**
 * A module that binds data specific to a single experiment.
 */
@Module
public final class ExperimentModule {
  private final ImmutableSortedMap<String, String> parameters;
  private final Method benchmarkMethod;

  private ExperimentModule(
      Method benchmarkMethod,
      ImmutableSortedMap<String, String> parameters) {
    this.parameters = checkNotNull(parameters);
    this.benchmarkMethod = checkNotNull(benchmarkMethod);
  }

  public static ExperimentModule forExperiment(Experiment experiment) {
    Method benchmarkMethod = experiment.instrumentation().benchmarkMethod();
    return new ExperimentModule(
        benchmarkMethod,
        experiment.userParameters());
  }

  public static ExperimentModule forWorkerSpec(WorkerSpec spec)
      throws ClassNotFoundException {
    Class<?> benchmarkClass = Util.loadClass(spec.benchmarkSpec.className());
    Method benchmarkMethod = findBenchmarkMethod(benchmarkClass, spec.benchmarkSpec.methodName(),
        spec.methodParameterClasses);
    benchmarkMethod.setAccessible(true);
    return new ExperimentModule(benchmarkMethod, spec.benchmarkSpec.parameters());
  }

  @Provides
  @Benchmark
  static Object provideRunningBenchmark(BenchmarkCreator creator) {
    return creator.createBenchmarkInstance();
  }

  @Provides
  @BenchmarkMethod
  String provideRunningBenchmarkMethodName() {
    return benchmarkMethod.getName();
  }

  @Provides
  @BenchmarkMethod
  Method provideRunningBenchmarkMethod() {
    return benchmarkMethod;
  }

  @Provides
  @Benchmark
  ImmutableSortedMap<String, String> provideUserParameters() {
    return parameters;
  }

  private static Method findBenchmarkMethod(Class<?> benchmark, String methodName,
      ImmutableList<Class<?>> methodParameterClasses) {
    Class<?>[] params = methodParameterClasses.toArray(new Class<?>[methodParameterClasses.size()]);
    try {
      return benchmark.getDeclaredMethod(methodName, params);
    } catch (NoSuchMethodException e) {
      throw new RuntimeException(e);
    } catch (SecurityException e) {
      // assertion error?
      throw new RuntimeException(e);
    }
  }
}
