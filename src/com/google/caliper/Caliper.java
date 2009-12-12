/**
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

package com.google.caliper;

import static com.google.common.base.Preconditions.checkArgument;

/**
 * Measure's the benchmark's per-trial execution time.
 */
class Caliper {

  private final long warmupNanos;
  private final long runNanos;

  public Caliper(long warmupMillis, long runMillis) {
    checkArgument(warmupMillis > 50);
    checkArgument(runMillis > 50);

    this.warmupNanos = warmupMillis * 1000000;
    this.runNanos = runMillis * 1000000;
  }

  public double warmUp(TimedRunnable timedRunnable) throws Exception {
    long startNanos = System.nanoTime();
    long endNanos = startNanos + warmupNanos;
    int trials = 0;
    long currentNanos;
    while ((currentNanos = System.nanoTime()) < endNanos) {
      timedRunnable.run(1);
      trials++;
    }
    double nanosPerExecution = (currentNanos - startNanos) / trials;
    if (nanosPerExecution > 1000000000 || nanosPerExecution < 2) {
      throw new ConfigurationException("Runtime out of range");
    }
    return nanosPerExecution;
  }

  /**
   * In the run proper, we predict how extrapolate based on warmup how many
   * runs we're going to need, and run them all in a single batch.
   */
  public double run(TimedRunnable test, double estimatedNanosPerTrial) throws Exception {
    int trials = (int) (runNanos / estimatedNanosPerTrial);
    if (trials == 0) {
      trials = 1;
    }
    long startNanos = System.nanoTime();
    test.run(trials);
    long endNanos = System.nanoTime();
    estimatedNanosPerTrial = (endNanos - startNanos) / trials;
    return estimatedNanosPerTrial;
  }
}