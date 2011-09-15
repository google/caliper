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

import static com.google.common.base.Throwables.propagateIfInstanceOf;

import com.google.caliper.api.Benchmark;
import com.google.caliper.api.SkipThisScenarioException;
import com.google.caliper.util.ShortDuration;
import com.google.caliper.worker.MicrobenchmarkWorker;
import com.google.caliper.worker.Worker;
import com.google.common.collect.ImmutableMap;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public final class MicrobenchmarkInstrument extends Instrument {

  @Override public ShortDuration estimateRuntimePerTrial() {
    return ShortDuration.valueOf(options.get("maxTotalRuntime"));
  }

  @Override public boolean isBenchmarkMethod(Method method) {
    return Instrument.isTimeMethod(method);
  }

  @Override public BenchmarkMethod createBenchmarkMethod(BenchmarkClass benchmarkClass,
      Method method) throws InvalidBenchmarkException {

    return Instrument.createBenchmarkMethodFromTimeMethod(benchmarkClass, method);
  }

  @Override public void dryRun(Benchmark benchmark, BenchmarkMethod benchmarkMethod)
      throws UserCodeException {
    Method m = benchmarkMethod.method();
    try {
      m.invoke(benchmark, 1);
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
        .put("warmupNanos", toNanosString("warmup"))
        .put("timingIntervalNanos", toNanosString("timingInterval"))
        .put("reportedIntervals", options.get("reportedIntervals"))
        .put("shortCircuitTolerance", options.get("shortCircuitTolerance"))
        .put("maxTotalRuntimeNanos", toNanosString("maxTotalRuntime"))
        .put("gcBeforeEach", options.get("gcBeforeEach"))
        .build();
  }

  @Override public Class<? extends Worker> workerClass() {
    return MicrobenchmarkWorker.class;
  }

  private String toNanosString(String optionName) {
    return String.valueOf(ShortDuration.valueOf(options.get(optionName)).to(TimeUnit.NANOSECONDS));
  }

  @Override public boolean equals(Object object) {
    return object instanceof MicrobenchmarkInstrument; // currently this class is stateless.
  }

  @Override public int hashCode() {
    return 0x5FE89C3A;
  }

  @Override public String toString() {
    return "micro";
  }
}
