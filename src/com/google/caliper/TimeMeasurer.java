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
class TimeMeasurer extends Measurer {

  private final long warmupNanos;
  private final long runNanos;

  /**
   * If the standard deviation of our measurements is within this tolerance, we
   * won't bother to perform additional measurements.
   */
  private final double SHORT_CIRCUIT_TOLERANCE = 0.01;

  private final int MAX_TRIALS = 10;

  TimeMeasurer(long warmupMillis, long runMillis, PrintStream verboseStream) {
    super(verboseStream);

    checkArgument(warmupMillis > 50);
    checkArgument(runMillis > 50);

    this.warmupNanos = warmupMillis * 1000000;
    this.runNanos = runMillis * 1000000;
  }

  private double warmUp(Supplier<ConfiguredBenchmark> testSupplier) throws Exception {
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
      long nanos = measureReps(testSupplier.get(), reps);
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
  private void checkScalesLinearly(Supplier<ConfiguredBenchmark> testSupplier) throws Exception {
    double half = measureReps(testSupplier.get(), Integer.MAX_VALUE / 2);
    double one = measureReps(testSupplier.get(), Integer.MAX_VALUE);
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
  @Override public MeasurementSet run(Supplier<ConfiguredBenchmark> testSupplier)
      throws Exception {
    double estimatedNanosPerRep = warmUp(testSupplier);

    log("[measuring nanos per rep with scale 1.00]");
    Measurement measurement100 = measure(testSupplier, 1.00, estimatedNanosPerRep);
    log("[measuring nanos per rep with scale 0.50]");
    Measurement measurement050 = measure(testSupplier, 0.50, measurement100.getRaw());
    log("[measuring nanos per rep with scale 1.50]");
    Measurement measurement150 = measure(testSupplier, 1.50, measurement100.getRaw());
    MeasurementSet measurementSet =
        new MeasurementSet(measurement100, measurement050, measurement150);

    for (int i = 3; i < MAX_TRIALS; i++) {
      double threshold = SHORT_CIRCUIT_TOLERANCE * measurementSet.meanRaw();
      if (measurementSet.standardDeviationRaw() < threshold) {
        return measurementSet;
      }

      log("[performing additional measurement with scale 1.00]");
      Measurement measurement = measure(testSupplier, 1.00, measurement100.getRaw());
      measurementSet = measurementSet.plusMeasurement(measurement);
    }

    return measurementSet;
  }

  /**
   * Runs the test method for approximately {@code runNanos * durationScale}
   * nanos and returns a Measurement of the nanos per rep and units per rep.
   */
  private Measurement measure(Supplier<ConfiguredBenchmark> testSupplier,
      double durationScale, double estimatedNanosPerRep) throws Exception {
    int reps = (int) (durationScale * runNanos / estimatedNanosPerRep);
    if (reps == 0) {
      reps = 1;
    }

    log("[running trial with " + reps + " reps]");
    ConfiguredBenchmark benchmark = testSupplier.get();
    long elapsedTime = measureReps(benchmark, reps);
    double nanosPerRep = elapsedTime / (double) reps;
    log(String.format("[took %.2f nanoseconds per rep]", nanosPerRep));
    return new Measurement(benchmark.timeUnitNames(), nanosPerRep,
        benchmark.nanosToUnits(nanosPerRep));
  }

  /**
   * Returns the total nanos to run {@code reps}.
   */
  private long measureReps(ConfiguredBenchmark benchmark, int reps) throws Exception {
    prepareForTest();
    log(LogConstants.MEASURED_SECTION_STARTING);
    long startNanos = System.nanoTime();
    benchmark.run(reps);
    long endNanos = System.nanoTime();
    log(LogConstants.MEASURED_SECTION_DONE);
    benchmark.close();
    return endNanos - startNanos;
  }
}
