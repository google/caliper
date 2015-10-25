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

import static java.math.RoundingMode.CEILING;
import static java.util.concurrent.TimeUnit.NANOSECONDS;

import com.google.caliper.util.ShortDuration;
import com.google.common.base.Ticker;
import com.google.common.math.LongMath;

/**
 * A utility that calculates the finest granularity that can be expected from subsequent calls to
 * {@link System#nanoTime()}.  Note that this utility necessarily invokes {@link System#nanoTime()}
 * directly rather than using {@link Ticker} because the extra indirection might cause additional
 * overhead.
 */
final class NanoTimeGranularityTester {
  private static final int TRIALS = 1000;

  ShortDuration testNanoTimeGranularity() {
    long total = 0L;
    for (int i = 0; i < TRIALS; i++) {
      long first = System.nanoTime();
      long second = System.nanoTime();
      long third = System.nanoTime();
      long fourth = System.nanoTime();
      long fifth = System.nanoTime();
      long sixth = System.nanoTime();
      long seventh = System.nanoTime();
      long eighth = System.nanoTime();
      long ninth = System.nanoTime();
      total += second - first;
      total += third - second;
      total += fourth - third;
      total += fifth - fourth;
      total += sixth - fifth;
      total += seventh - sixth;
      total += eighth - seventh;
      total += ninth - eighth;
    }
    return ShortDuration.of(LongMath.divide(total, TRIALS * 8, CEILING), NANOSECONDS);
  }
}
