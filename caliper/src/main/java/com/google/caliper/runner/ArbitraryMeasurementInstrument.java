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

import com.google.caliper.api.SkipThisScenarioException;
import com.google.caliper.bridge.AbstractLogMessageVisitor;
import com.google.caliper.bridge.StopMeasurementLogMessage;
import com.google.caliper.model.ArbitraryMeasurement;
import com.google.caliper.model.Measurement;
import com.google.caliper.platform.Platform;
import com.google.caliper.platform.SupportedPlatform;
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
@SupportedPlatform(Platform.Type.JVM)
public final class ArbitraryMeasurementInstrument extends Instrument {
  @Override public boolean isBenchmarkMethod(Method method) {
    return method.isAnnotationPresent(ArbitraryMeasurement.class);
  }

  @Override
  public Instrumentation createInstrumentation(Method benchmarkMethod)
      throws InvalidBenchmarkException {
    if (benchmarkMethod.getParameterTypes().length != 0) {
      throw new InvalidBenchmarkException(
          "Arbitrary measurement methods should take no parameters: " + benchmarkMethod.getName());
    }

    if (benchmarkMethod.getReturnType() != double.class) {
      throw new InvalidBenchmarkException(
          "Arbitrary measurement methods must have a return type of double: "
              + benchmarkMethod.getName());
    }

    // Static technically doesn't hurt anything, but it's just the completely wrong idea
    if (Util.isStatic(benchmarkMethod)) {
      throw new InvalidBenchmarkException(
          "Arbitrary measurement methods must not be static: " + benchmarkMethod.getName());
    }

    if (!Util.isPublic(benchmarkMethod)) {
      throw new InvalidBenchmarkException(
          "Arbitrary measurement methods must be public: " + benchmarkMethod.getName());
    }

    return new ArbitraryMeasurementInstrumentation(benchmarkMethod);
  }

  @Override public TrialSchedulingPolicy schedulingPolicy() {
    // We could allow it here but in general it would depend on the particular measurement so it
    // should probably be configured by the user.  For now we just disable it.
    return TrialSchedulingPolicy.SERIAL;
  }

  private final class ArbitraryMeasurementInstrumentation extends Instrumentation {
    protected ArbitraryMeasurementInstrumentation(Method benchmarkMethod) {
      super(benchmarkMethod);
    }

    @Override
    public void dryRun(Object benchmark) throws InvalidBenchmarkException {
      try {
        benchmarkMethod.invoke(benchmark);
      } catch (IllegalAccessException impossible) {
        throw new AssertionError(impossible);
      } catch (InvocationTargetException e) {
        Throwable userException = e.getCause();
        propagateIfInstanceOf(userException, SkipThisScenarioException.class);
        throw new UserCodeException(userException);
      }
    }

    @Override
    public Class<? extends Worker> workerClass() {
      return ArbitraryMeasurementWorker.class;
    }

    @Override public ImmutableMap<String, String> workerOptions() {
      return ImmutableMap.of(GC_BEFORE_EACH_OPTION, options.get(GC_BEFORE_EACH_OPTION));
    }

    @Override
    MeasurementCollectingVisitor getMeasurementCollectingVisitor() {
      return new SingleMeasurementCollectingVisitor();
    }
  }

  @Override
  public ImmutableSet<String> instrumentOptions() {
    return ImmutableSet.of(GC_BEFORE_EACH_OPTION);
  }

  private static final class SingleMeasurementCollectingVisitor extends AbstractLogMessageVisitor
      implements MeasurementCollectingVisitor {
    Optional<Measurement> measurement = Optional.absent();

    @Override
    public boolean isDoneCollecting() {
      return measurement.isPresent();
    }

    @Override
    public boolean isWarmupComplete() {
      return true;
    }

    @Override
    public ImmutableList<Measurement> getMeasurements() {
      return ImmutableList.copyOf(measurement.asSet());
    }

    @Override
    public void visit(StopMeasurementLogMessage logMessage) {
      this.measurement = Optional.of(Iterables.getOnlyElement(logMessage.measurements()));
    }

    @Override 
    public ImmutableList<String> getMessages() {
      return ImmutableList.of();
    }
  }
}
