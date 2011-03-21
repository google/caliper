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
import com.google.caliper.util.SimpleDuration;
import com.google.caliper.util.Util;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Map;

public final class MicrobenchmarkInstrument extends Instrument {

  @Override protected void setOptions(Map<String, String> options) {
    super.setOptions(options);
  }

  @Override public SimpleDuration estimateRuntimePerTrial() {
    SimpleDuration minWarmup = SimpleDuration.valueOf(options.get("minWarmup"));
    SimpleDuration maxRuntime = SimpleDuration.valueOf(options.get("maxRuntime"));

    return minWarmup.plus(maxRuntime);
  }

  @Override public boolean isBenchmarkMethod(Method method) {
    // Just skip over private methods
    return method.getName().startsWith("time") && Util.isPublic(method);
  }

  @Override public BenchmarkMethod createBenchmarkMethod(
      BenchmarkClass benchmarkClass, Method method) throws InvalidBenchmarkException {
    if (!Arrays.equals(method.getParameterTypes(), new Class<?>[] {int.class})) {
      throw new InvalidBenchmarkException(
          "Microbenchmark methods must accept a single int parameter: " + method);
    }

    String methodName = method.getName();
    String shortName = methodName.substring("time".length());
    return new BenchmarkMethod(benchmarkClass, method, shortName);
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

  public void measure(Benchmark benchmark, BenchmarkMethod benchmarkMethod)
      throws UserCodeException {
    
  }

  @Override public boolean equals(Object object) {
    return object instanceof MicrobenchmarkInstrument; // currently this class is stateless.
  }

  @Override public int hashCode() {
    return 0x5FE89C3A;
  }

  @Override public String toString() {
    return "microbenchmark";
  }
}
