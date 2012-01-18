// Copyright 2011 Google Inc. All Rights Reserved.

package com.google.caliper.runner;

import static com.google.common.base.Throwables.propagateIfInstanceOf;

import com.google.caliper.api.Benchmark;
import com.google.caliper.api.SkipThisScenarioException;
import com.google.caliper.model.ArbitraryMeasurement;
import com.google.caliper.util.Util;
import com.google.caliper.worker.ArbitraryMeasurementWorker;
import com.google.caliper.worker.Worker;
import com.google.common.collect.ImmutableMap;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Map;

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

  @Override public Map<String, String> workerOptions() {
    return new ImmutableMap.Builder<String, String>()
        .put("gcBeforeEach", options.get("gcBeforeEach"))
        .build();
  }

  @Override public Class<? extends Worker> workerClass() {
    return ArbitraryMeasurementWorker.class;
  }
}
