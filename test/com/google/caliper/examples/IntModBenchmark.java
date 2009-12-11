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

package com.google.caliper.examples;

import com.google.caliper.Runner;
import com.google.caliper.SimpleBenchmark;

/**
 * Measures several candidate implementations for mod().
 */
public class IntModBenchmark extends SimpleBenchmark {
  private static final int M = (1 << 16) - 1;

  public int timeConditional(int reps) {
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

  public int timeDoubleRemainder(int reps) {
    int dummy = 5;
    for (int i = 0; i < reps; i++) {
      dummy += Integer.MAX_VALUE + doubleRemainderMod(dummy, M);
    }
    return dummy;
  }

  private static int doubleRemainderMod(int a, int m) {
    return (int) (((a % m) + (long) m) % m);
  }

  public int timeRightShiftingMod(int reps) {
    int dummy = 5;
    for (int i = 0; i < reps; i++) {
      dummy += Integer.MAX_VALUE + rightShiftingMod(dummy, M);
    }
    return dummy;
  }

  private static int rightShiftingMod(int a, int m) {
     long r = a % m;
     return (int) (r + ((r >> 63) & m));
  }

  public int timeLeftShiftingMod(int reps) {
    int dummy = 5;
    for (int i = 0; i < reps; i++) {
      dummy += Integer.MAX_VALUE + leftShiftingMod(dummy, M);
    }
    return dummy;
  }

  private static int leftShiftingMod(int a, int m) {
    return (int) ((a + (((long) m) << 32)) % m);
  }

  public int timeWrongMod(int reps) {
    int dummy = 5;
    for (int i = 0; i < reps; i++) {
      dummy += Integer.MAX_VALUE + dummy % M;
    }
    return dummy;
  }

  // TODO: remove this from all examples when IDE plugins are ready
  public static void main(String[] args) throws Exception {
    Runner.main(IntModBenchmark.class, args);
  }
}