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
import com.google.common.base.Supplier;

/**
 * Measure's the benchmark's per-trial execution time.
 */
class Caliper {

  private final long warmupNanos;
  private final long runNanos;

  /**
   * If the standard deviation of our measurements is within this tolerance, we
   * won't bother to perform additional measurements.
   */
  private final double SHORT_CIRCUIT_TOLERANCE = 0.01;

  private final int MAX_TRIALS = 10;

  Caliper(long warmupMillis, long runMillis) {
    checkArgument(warmupMillis > 50);
    checkArgument(runMillis > 50);

    this.warmupNanos = warmupMillis * 1000000;
    this.runNanos = runMillis * 1000000;
  }

  public double warmUp(Supplier<TimedRunnable> testSupplier) throws Exception {
    long elapsedNanos = 0;
    int netReps = 0;
    int reps = 1;

    /*
     * Run progressively more reps at a time until we cross our warmup
     * threshold. This way any just-in-time compiler will be comfortable running
     * multiple iterations of our measurement method.
     */
    while (elapsedNanos < warmupNanos) {
      TimedRunnable test = testSupplier.get(); // construct the test when we're off-the-clock

      prepareForTest();
      long startNanos = System.nanoTime();
      test.run(reps);
      long endNanos = System.nanoTime();
      test.close();

      elapsedNanos += (endNanos - startNanos);
      netReps += reps;
      reps *= 2;
    }

    double nanosPerExecution = (elapsedNanos) / (double) netReps;
    if (nanosPerExecution > 1000000000 || nanosPerExecution < 2) {
      throw new ConfigurationException("Runtime " + nanosPerExecution + " out of range");
    }
    return nanosPerExecution;
  }

  /**
   * Measure the nanos per rep for the given test. This code uses an interesting
   * strategy to measure the runtime to minimize execution time when execution
   * time is consistent.
   * <ol>
   *   <li>1.0x {@code runMillis} trial is run.
   *   <li>0.5x {@code runMillis} trial is run.
   *   <li>1.5x {@code runMillis} trial is run.
   *   <li>At this point, the standard deviation of these trials is computed. If
   *       it is within the threshold, the result is returned.
   *   <li>Otherwise trials continue to be executed until either the threshold
   *       is satisfied or the maximum number of runs have been executed.
   * </ol>
   *
   * @param testSupplier provides instances of the code under test. A new test
   *      is created for each iteration because some benchmarks' performance
   *      depends on which memory was allocated. See SetContainsBenchmark for an
   *      example.
   */
  public MeasurementSet run(Supplier<TimedRunnable> testSupplier, double estimatedNanosPerTrial)
      throws Exception {
    double npr100 = measure(testSupplier, 1.00, estimatedNanosPerTrial);
    double npr050 = measure(testSupplier, 0.50, npr100);
    double npr150 = measure(testSupplier, 1.50, npr100);
    MeasurementSet measurementSet = new MeasurementSet(npr100, npr050, npr150);

    for (int i = 3; i < MAX_TRIALS; i++) {
      double threshold = SHORT_CIRCUIT_TOLERANCE * measurementSet.mean();
      if (measurementSet.standardDeviation() < threshold) {
        return measurementSet;
      }

      double npr = measure(testSupplier, 1.0, npr100);
      measurementSet = measurementSet.plusMeasurement(npr);
    }

    return measurementSet;
  }

  /**
   * Runs the test method for approximately {@code runNanos * durationScale}
   * nanos and returns the nanos per rep.
   */
  private double measure(Supplier<TimedRunnable> testSupplier,
      double durationScale, double estimatedNanosPerTrial) throws Exception {
    TimedRunnable test = testSupplier.get();

    int trials = (int) (durationScale * runNanos / estimatedNanosPerTrial);
    if (trials == 0) {
      trials = 1;
    }

    prepareForTest();
    long startNanos = System.nanoTime();
    test.run(trials);
    long elapsedTime = System.nanoTime() - startNanos;
    test.close();

    return elapsedTime / (double) trials;
  }

  private void prepareForTest() {
    System.gc();
    System.gc();
  }
}
