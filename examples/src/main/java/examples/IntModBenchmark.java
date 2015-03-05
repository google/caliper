/*
 * Copyright (C) 2009 Google Inc.
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

package examples;

import com.google.caliper.Benchmark;

/**
 * Measures several candidate implementations for mod().
 */
@SuppressWarnings("SameParameterValue")
public class IntModBenchmark {
  private static final int M = (1 << 16) - 1;

  @Benchmark int conditional(int reps) {
    int dummy = 5;
    for (int i = 0; i < reps; i++) {
      dummy += Integer.MAX_VALUE + conditionalMod(dummy, M);
    }
    return dummy;
  }

  private static int conditionalMod(int a, int m) {
    int r = a % m;
    return r < 0 ? r + m : r;
  }

  @Benchmark int doubleRemainder(int reps) {
    int dummy = 5;
    for (int i = 0; i < reps; i++) {
      dummy += Integer.MAX_VALUE + doubleRemainderMod(dummy, M);
    }
    return dummy;
  }

  @SuppressWarnings("NumericCastThatLosesPrecision") // result of % by an int must be in int range
  private static int doubleRemainderMod(int a, int m) {
    return (int) ((a % m + (long) m) % m);
  }

  @Benchmark int rightShiftingMod(int reps) {
    int dummy = 5;
    for (int i = 0; i < reps; i++) {
      dummy += Integer.MAX_VALUE + rightShiftingMod(dummy, M);
    }
    return dummy;
  }

  @SuppressWarnings("NumericCastThatLosesPrecision") // must be in int range
  private static int rightShiftingMod(int a, int m) {
     long r = a % m;
     return (int) (r + (r >> 63 & m));
  }

  @Benchmark int leftShiftingMod(int reps) {
    int dummy = 5;
    for (int i = 0; i < reps; i++) {
      dummy += Integer.MAX_VALUE + leftShiftingMod(dummy, M);
    }
    return dummy;
  }

  @SuppressWarnings("NumericCastThatLosesPrecision") // result of % by an int must be in int range
  private static int leftShiftingMod(int a, int m) {
    return (int) ((a + ((long) m << 32)) % m);
  }

  @Benchmark int wrongMod(int reps) {
    int dummy = 5;
    for (int i = 0; i < reps; i++) {
      dummy += Integer.MAX_VALUE + dummy % M;
    }
    return dummy;
  }
}
