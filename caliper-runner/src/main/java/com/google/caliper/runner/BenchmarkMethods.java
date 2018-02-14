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
import com.google.caliper.core.BenchmarkClassModel.MethodModel;
import com.google.caliper.core.InvalidBenchmarkException;
import com.google.common.collect.ImmutableList;
import java.lang.reflect.Modifier;

/** Utilities for working with methods annotated by {@link Benchmark}. */
final class BenchmarkMethods {
  private static final ImmutableList<String> MACROBENCHMARK_PARAMS = ImmutableList.of();
  private static final ImmutableList<String> MICROBENCHMARK_PARAMS = ImmutableList.of("int");
  private static final ImmutableList<String> PICOBENCHMARK_PARAMS = ImmutableList.of("long");

  private BenchmarkMethods() {}

  enum Type {
    MACRO,
    MICRO,
    PICO;

    static Type of(MethodModel benchmarkMethod) {
      ImmutableList<String> parameterTypes = benchmarkMethod.parameterTypes();
      if (parameterTypes.equals(MACROBENCHMARK_PARAMS)) {
        return MACRO;
      } else if (parameterTypes.equals(MICROBENCHMARK_PARAMS)) {
        return MICRO;
      } else if (parameterTypes.equals(PICOBENCHMARK_PARAMS)) {
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
  static boolean isTimeMethod(MethodModel method) {
    return method.name().startsWith("time") && Modifier.isPublic(method.modifiers());
  }

  /**
   * For instruments that use {@link #isTimeMethod} to identify their methods, this method checks
   * the {@link Method} appropriately.
   */
  static MethodModel checkTimeMethod(MethodModel timeMethod) throws InvalidBenchmarkException {
    checkArgument(isTimeMethod(timeMethod));
    ImmutableList<String> parameterTypes = timeMethod.parameterTypes();
    if (!parameterTypes.equals(MICROBENCHMARK_PARAMS)
        && !parameterTypes.equals(PICOBENCHMARK_PARAMS)) {
      throw new InvalidBenchmarkException(
          "Microbenchmark methods must accept a single int parameter: " + timeMethod.name());
    }

    // Static technically doesn't hurt anything, but it's just the completely wrong idea
    if (Modifier.isStatic(timeMethod.modifiers())) {
      throw new InvalidBenchmarkException(
          "Microbenchmark methods must not be static: " + timeMethod.name());
    }
    return timeMethod;
  }
}
