/*
 * Copyright (C) 2011 Google Inc.
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

import static com.google.caliper.runner.CommonInstrumentOptions.GC_BEFORE_EACH_OPTION;
import static com.google.common.base.Throwables.propagateIfInstanceOf;

import com.google.caliper.Benchmark;
import com.google.caliper.api.SkipThisScenarioException;
import com.google.caliper.bridge.AbstractLogMessageVisitor;
import com.google.caliper.bridge.StopTimingLogMessage;
import com.google.caliper.model.ArbitraryMeasurement;
import com.google.caliper.model.Measurement;
import com.google.caliper.util.Util;
import com.google.caliper.worker.ArbitraryMeasurementWorker;
import com.google.caliper.worker.Worker;
import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * Instrument for taking an arbitrary measurement. When using this instrument, the benchmark code
 * itself returns the value. See {@link ArbitraryMeasurement}.
 */
public final class ArbitraryMeasurementInstrument extends Instrument {
  @Override public boolean isBenchmarkMethod(Method method) {
    return method.isAnnotationPresent(ArbitraryMeasurement.class);
  }

  @Override
  public BenchmarkMethod createBenchmarkMethod(BenchmarkClass benchmarkClass, Method method)
      throws InvalidBenchmarkException {

    if (method.getParameterTypes().length != 0) {
      throw new InvalidBenchmarkException(
          "Arbitrary measurement methods should take no parameters: " + method.getName());
    }

    if (method.getReturnType() != double.class) {
      throw new InvalidBenchmarkException(
          "Arbitrary measurement methods must have a return type of double: " + method.getName());
    }

    // Static technically doesn't hurt anything, but it's just the completely wrong idea
    if (Util.isStatic(method)) {
      throw new InvalidBenchmarkException(
          "Arbitrary measurement methods must not be static: " + method.getName());
    }

    if (!Util.isPublic(method)) {
      throw new InvalidBenchmarkException(
          "Arbitrary measurement methods must be public: " + method.getName());
    }

    return new BenchmarkMethod(benchmarkClass, method, method.getName());
  }

  @Override
  public void dryRun(Benchmark benchmark, BenchmarkMethod benchmarkMethod)
      throws UserCodeException {

    Method m = benchmarkMethod.method();
    try {
      m.invoke(benchmark);
    } catch (IllegalAccessException impossible) {
      throw new AssertionError(impossible);
    } catch (InvocationTargetException e) {
      Throwable userException = e.getCause();
      propagateIfInstanceOf(userException, SkipThisScenarioException.class);
      throw new UserCodeException(userException);
    }
  }

  @Override
  public ImmutableSet<String> instrumentOptions() {
    return ImmutableSet.of(GC_BEFORE_EACH_OPTION);
  }

  @Override public ImmutableMap<String, String> workerOptions() {
    return new ImmutableMap.Builder<String, String>()
        .put(GC_BEFORE_EACH_OPTION, options.get(GC_BEFORE_EACH_OPTION))
        .build();
  }

  @Override public Class<? extends Worker> workerClass() {
    return ArbitraryMeasurementWorker.class;
  }

  @Override
  MeasurementCollectingVisitor getMeasurementCollectingVisitor() {
    return new SingleMeasurementCollectingVisitor();
  }

  private static final class SingleMeasurementCollectingVisitor extends AbstractLogMessageVisitor
      implements MeasurementCollectingVisitor {
    Optional<Measurement> measurement = Optional.absent();

    @Override
    public boolean isDoneCollecting() {
      return measurement.isPresent();
    }

    @Override
    public ImmutableList<Measurement> getMeasurements() {
      return ImmutableList.copyOf(measurement.asSet());
    }

    @Override
    public void visit(StopTimingLogMessage logMessage) {
      this.measurement = Optional.of(Iterables.getOnlyElement(logMessage.measurements()));
    }
  }
}
