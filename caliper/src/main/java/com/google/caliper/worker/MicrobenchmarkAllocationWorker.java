/*
 * Copyright (C) 2011 Google Inc.
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

package com.google.caliper.worker;

import com.google.caliper.model.Measurement;
import com.google.caliper.runner.Running.Benchmark;
import com.google.caliper.runner.Running.BenchmarkMethod;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import javax.inject.Inject;

/**
 * The {@link Worker} for the {@code AllocationInstrument}.  This class invokes the benchmark method
 * a few times, with varying numbers of reps, and computes the number of object allocations and the
 * total size of those allocations.
 */
public final class MicrobenchmarkAllocationWorker extends Worker {
  // TODO(gak): make this or something like this an option
  private static final int WARMUP_REPS = 10;
  private static final int MAX_REPS = 100;

  /**
   * The number of consecutive measurement runs that must have matching allocations during the warm
   * up in order for the method to be determined to be deterministic.
   */
  private static final int DETERMINISTIC_BENCHMARK_THRESHOLD = 2;

  /**
   * The maximum number of warm up measurements to take before determining that the test is
   * non-deterministic.
   */
  private static final int DETERMINISTIC_MEASUREMENT_COUNT = DETERMINISTIC_BENCHMARK_THRESHOLD + 3;

  private static final String LINE_SEPARATOR = System.getProperty("line.separator");

  private final Random random;
  private final AllocationRecorder recorder;

  @Inject MicrobenchmarkAllocationWorker(@Benchmark Object benchmark,
      @BenchmarkMethod Method method, AllocationRecorder recorder, Random random) {
    super(benchmark, method);
    this.random = random;
    this.recorder = recorder;
  }

  @Override public void bootstrap() throws Exception {
    // do some initial measurements and throw away the results. this warms up the bootstrap method
    // itself and also the method invocation path for calling that method.

    // warm up the loop in the benchmark method.
    measureAllocations(benchmark, benchmarkMethod, WARMUP_REPS);

    // verify that the benchmark is deterministic in terms of the measured allocations.
    verifyBenchmarkIsDeterministic();
  }

  /**
   * Verify the determinism of the benchmark method.
   *
   * <p>The method invocation path, i.e. the code that the JVM executes to invoke the method, can
   * vary depending on how many times it is run with a corresponding effect on the allocations
   * measured. The invocations performed by this method should be sufficient to cause the JVM to
   * settle on a single path for invoking the benchmark method and so cause identical allocations
   * for each subsequent invocation. If tests start to fail with lots of non-deterministic
   * allocation errors then it's possible that additional invocations are required in which case
   * the value of {@link #DETERMINISTIC_BENCHMARK_THRESHOLD} should be increased.
   */
  private void verifyBenchmarkIsDeterministic() throws Exception {
    // keep track of all the statistics generated while warming up the method invocation path.
    List<AllocationStats> history = new ArrayList<AllocationStats>();

    // warm up the method invocation path by calling the benchmark multiple times with 0 reps.
    AllocationStats baseline = null;
    int matchingSequenceLength = 1;
    for (int i = 0; i < DETERMINISTIC_MEASUREMENT_COUNT; ++i) {
      AllocationStats stats = measureAllocations(benchmark, benchmarkMethod, 0);
      history.add(stats);
      if (stats.equals(baseline)) {
        // if consecutive measurements with the same allocation characteristics reaches the
        // threshold then treat the benchmark as being deterministic.
        if (++matchingSequenceLength == DETERMINISTIC_BENCHMARK_THRESHOLD) {
          return;
        }
      } else {
        matchingSequenceLength = 1;
        baseline = stats;
      }
    }

    // the baseline allocations did not settle down and so are probably non-deterministic.
    StringBuilder builder = new StringBuilder(100);
    AllocationStats previous = null;
    for (AllocationStats allocationStats : history) {
      if (previous == null) {
        builder.append(LINE_SEPARATOR).append("  ").append(allocationStats);
      } else {
        AllocationStats.Delta delta = allocationStats.delta(previous);
        builder.append(LINE_SEPARATOR).append("  ").append(delta);
      }
      previous = allocationStats;
    }
    throw new IllegalStateException(String.format(
        "Your benchmark appears to have non-deterministic allocation behavior. "
        + "During the warm up process there was no consecutive sequence of %d runs with"
        + " identical allocations. The allocation history is:%s",
        DETERMINISTIC_BENCHMARK_THRESHOLD, builder));
  }

  @Override public Iterable<Measurement> measure() throws Exception {
    AllocationStats baseline = measureAllocations(benchmark, benchmarkMethod, 0);
    // [1, MAX_REPS]
    int measurementReps = random.nextInt(MAX_REPS) + 1;
    AllocationStats measurement = measureAllocations(benchmark, benchmarkMethod, measurementReps);
    return measurement.minus(baseline).toMeasurements();
  }

  private AllocationStats measureAllocations(
      Object benchmark, Method method, int reps) throws Exception {
    // do the Integer boxing and the creation of the Object[] outside of the record block, so that
    // our internal allocations aren't counted in the benchmark's allocations.
    Object[] args = {reps};
    recorder.startRecording();
    method.invoke(benchmark, args);
    return recorder.stopRecording(reps);
  }
}
