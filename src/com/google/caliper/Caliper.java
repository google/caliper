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


import com.google.caliper.UserException.DoesNotScaleLinearlyException;
import com.google.caliper.UserException.RuntimeOutOfRangeException;
import static com.google.common.base.Preconditions.checkArgument;
import com.google.common.base.Supplier;
import java.io.PrintStream;

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

  private final PrintStream verboseStream;

  Caliper(long warmupMillis, long runMillis, PrintStream verboseStream) {
    checkArgument(warmupMillis > 50);
    checkArgument(runMillis > 50);

    this.warmupNanos = warmupMillis * 1000000;
    this.runNanos = runMillis * 1000000;
    this.verboseStream = verboseStream;
  }

  public double warmUp(Supplier<TimedRunnable> testSupplier) throws Exception {
    long elapsedNanos = 0;
    long netReps = 0;
    int reps = 1;
    boolean definitelyScalesLinearly = false;

    /*
     * Run progressively more reps at a time until we cross our warmup
     * threshold. This way any just-in-time compiler will be comfortable running
     * multiple iterations of our measurement method.
     */
    log("[starting warmup]");
    while (elapsedNanos < warmupNanos) {
      long nanos = measureReps(testSupplier, reps);
      elapsedNanos += nanos;

      netReps += reps;
      reps *= 2;

      // if reps overflowed, that's suspicious! Check that it time scales with reps
      if (reps <= 0 && !definitelyScalesLinearly) {
        checkScalesLinearly(testSupplier);
        definitelyScalesLinearly = true;
        reps = Integer.MAX_VALUE;
      }
    }
    log("[ending warmup]");

    double nanosPerExecution = (double) elapsedNanos / netReps;
    double lowerBound = 0.1;
    double upperBound = 10000000000.0;
    if (!(lowerBound <= nanosPerExecution && nanosPerExecution <= upperBound)) {
      throw new RuntimeOutOfRangeException(nanosPerExecution, lowerBound, upperBound);
    }

    return nanosPerExecution;
  }

  /**
   * Doing half as much work shouldn't take much more than half as much time. If
   * it does we have a broken benchmark!
   */
  private void checkScalesLinearly(Supplier<TimedRunnable> testSupplier) throws Exception {
    double half = measureReps(testSupplier, Integer.MAX_VALUE / 2);
    double one = measureReps(testSupplier, Integer.MAX_VALUE);
    if (half / one > 0.75) {
      throw new DoesNotScaleLinearlyException();
    }
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
  public MeasurementSet run(Supplier<TimedRunnable> testSupplier, double estimatedNanosPerRep)
      throws Exception {
    log("[measuring nanos per rep with scale 1.00]");
    double npr100 = measure(testSupplier, 1.00, estimatedNanosPerRep);
    log("[measuring nanos per rep with scale 0.50]");
    double npr050 = measure(testSupplier, 0.50, npr100);
    log("[measuring nanos per rep with scale 1.50]");
    double npr150 = measure(testSupplier, 1.50, npr100);
    MeasurementSet measurementSet = new MeasurementSet(npr100, npr050, npr150);

    for (int i = 3; i < MAX_TRIALS; i++) {
      double threshold = SHORT_CIRCUIT_TOLERANCE * measurementSet.mean();
      if (measurementSet.standardDeviation() < threshold) {
        return measurementSet;
      }

      log("[performing additional measurement with scale 1.00]");
      double npr = measure(testSupplier, 1.00, npr100);
      measurementSet = measurementSet.plusMeasurement(npr);
    }

    return measurementSet;
  }

  /**
   * Runs the test method for approximately {@code runNanos * durationScale}
   * nanos and returns the nanos per rep.
   */
  private double measure(Supplier<TimedRunnable> testSupplier,
      double durationScale, double estimatedNanosPerRep) throws Exception {
    int reps = (int) (durationScale * runNanos / estimatedNanosPerRep);
    if (reps == 0) {
      reps = 1;
    }

    log("[running trial with " + reps + " reps]");
    long elapsedTime = measureReps(testSupplier, reps);
    double nanosPerRep = elapsedTime / (double) reps;
    log(String.format("[took %.2f nanoseconds per rep]", nanosPerRep));
    return nanosPerRep;
  }

  /**
   * Returns the total nanos to run {@code reps}.
   */
  private long measureReps(Supplier<TimedRunnable> testSupplier, int reps) throws Exception {
    TimedRunnable test = testSupplier.get();
    prepareForTest();
    log(LogConstants.TIMED_SECTION_STARTING);
    long startNanos = System.nanoTime();
    test.run(reps);
    long endNanos = System.nanoTime();
    log(LogConstants.TIMED_SECTION_DONE);
    test.close();
    return endNanos - startNanos;
  }

  private void prepareForTest() {
    System.gc();
    System.gc();
  }

  private void log(String message) {
    verboseStream.println(LogConstants.CALIPER_LOG_PREFIX + message);
  }
}
