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

import static com.google.common.base.Preconditions.checkArgument;

import com.google.caliper.Benchmark;
import com.google.caliper.util.Util;

import java.lang.reflect.Method;
import java.util.Arrays;

/**
 * Utilities for working with methods annotated by {@link Benchmark}.
 */
final class BenchmarkMethods {
  private static final Class<?>[] MACROBENCHMARK_PARAMS = new Class<?>[] {};
  private static final Class<?>[] MICROBENCHMARK_PARAMS = new Class<?>[] {int.class};
  private static final Class<?>[] PICOBENCHMARK_PARAMS = new Class<?>[] {long.class};

  private BenchmarkMethods() {}

  enum Type {
    MACRO,
    MICRO,
    PICO;

    static Type of(Method benchmarkMethod) {
      Class<?>[] parameterTypes = benchmarkMethod.getParameterTypes();
      if (Arrays.equals(parameterTypes, MACROBENCHMARK_PARAMS)) {
        return MACRO;
      } else if (Arrays.equals(parameterTypes, MICROBENCHMARK_PARAMS)) {
        return MICRO;
      } else if (Arrays.equals(parameterTypes, PICOBENCHMARK_PARAMS)) {
        return PICO;
      } else {
        throw new IllegalArgumentException("invalid method parameters: " + benchmarkMethod);
      }
    }
  }

  /**
   * Several instruments look for benchmark methods like {@code timeBlah(int reps)}; this is the
   * centralized code that identifies such methods.
   *
   * <p>This method does not check the correctness of the argument types.
   */
  static boolean isTimeMethod(Method method) {
    return method.getName().startsWith("time") && Util.isPublic(method);
  }

  /**
   * For instruments that use {@link #isTimeMethod} to identify their methods, this method checks
   * the {@link Method} appropriately.
   */
  static Method checkTimeMethod(Method timeMethod) throws InvalidBenchmarkException {
    checkArgument(isTimeMethod(timeMethod));
    Class<?>[] parameterTypes = timeMethod.getParameterTypes();
    if (!Arrays.equals(parameterTypes, new Class<?>[] {int.class})
        && !Arrays.equals(parameterTypes, new Class<?>[] {long.class})) {
      throw new InvalidBenchmarkException(
          "Microbenchmark methods must accept a single int parameter: " + timeMethod.getName());
    }

    // Static technically doesn't hurt anything, but it's just the completely wrong idea
    if (Util.isStatic(timeMethod)) {
      throw new InvalidBenchmarkException(
          "Microbenchmark methods must not be static: " + timeMethod.getName());
    }
    return timeMethod;
  }
}
