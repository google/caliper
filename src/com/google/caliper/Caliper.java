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

  Caliper(long warmupMillis, long runMillis) {
    checkArgument(warmupMillis > 50);
    checkArgument(runMillis > 50);

    this.warmupNanos = warmupMillis * 1000000;
    this.runNanos = runMillis * 1000000;
  }

  public double warmUp(TimedRunnable timedRunnable) throws Exception {
    long startNanos = System.nanoTime();
    long endNanos = startNanos + warmupNanos;
    long currentNanos;
    int netReps = 0;
    int reps = 1;

    /*
     * Run progressively more reps at a time until we cross our warmup
     * threshold. This way any just-in-time compiler will be comfortable running
     * multiple iterations of our measurement method.
     */
    while ((currentNanos = System.nanoTime()) < endNanos) {
      timedRunnable.run(reps);
      netReps += reps;
      reps *= 2;
    }

    double nanosPerExecution = (currentNanos - startNanos) / (double) netReps;
    if (nanosPerExecution > 1000000000 || nanosPerExecution < 2) {
      throw new ConfigurationException("Runtime " + nanosPerExecution + " out of range");
    }
    return nanosPerExecution;
  }

  /**
   * In the run proper, we predict how extrapolate based on warmup how many
   * runs we're going to need, and run them all in a single batch.
   */
  public double run(TimedRunnable test, double estimatedNanosPerTrial)
      throws Exception {
    @SuppressWarnings("NumericCastThatLosesPrecision")
    int trials = (int) (runNanos / estimatedNanosPerTrial);
    if (trials == 0) {
      trials = 1;
    }

    double nanosPerTrial = measure(test, trials);

    // if the runtime was in the expected range, return it. We're good.
    if (isPlausible(estimatedNanosPerTrial, nanosPerTrial)) {
      return nanosPerTrial;
    }

    // The runtime was outside of the expected range. Perhaps the VM is inlining
    // things too aggressively? We'll run more rounds to confirm that the
    // runtime scales with the number of trials.
    double nanosPerTrial2 = measure(test, trials * 4);
    if (isPlausible(nanosPerTrial, nanosPerTrial2)) {
      return nanosPerTrial;
    }

    throw new ConfigurationException("Measurement error: "
        + "runtime isn't proportional to the number of repetitions!");
  }

  /**
   * Returns true if the given measurement is consistent with the expected
   * measurement.
   */
  private boolean isPlausible(double expected, double measurement) {
    double ratio = measurement / expected;
    return ratio > 0.9 && ratio < 1.1;
  }

  private double measure(TimedRunnable test, int trials) throws Exception {
    prepareForTest();
    long startNanos = System.nanoTime();
    test.run(trials);
    return (System.nanoTime() - startNanos) / (double) trials;
  }

  private void prepareForTest() {
    System.gc();
    System.gc();
  }
}