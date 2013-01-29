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

import com.google.caliper.Benchmark;
import com.google.caliper.model.Measurement;
import com.google.caliper.model.Measurement.Builder;
import com.google.caliper.model.Value;
import com.google.caliper.util.Util;
import com.google.inject.Inject;

import java.lang.reflect.Method;
import java.util.Map;
import java.util.Random;
import java.util.logging.Logger;

public class MicrobenchmarkWorker implements Worker {
  private static final Logger logger = Logger.getLogger(MicrobenchmarkWorker.class.getName());
  private static final int INITIAL_REPS = 30;

  private final Random random;

  @Inject MicrobenchmarkWorker(Random random) {
    this.random = random;
  }

  @Override public void measure(Benchmark benchmark, String methodName,
      Map<String, String> optionMap, WorkerEventLog log) throws Exception {
    Options options = new Options(optionMap);
    Trial trial = new Trial(benchmark, methodName, options, log);

    long warmupNanos = trial.warmUp(INITIAL_REPS);

    trial.run(INITIAL_REPS, warmupNanos);
  }

  private class Trial {
    final Benchmark benchmark;
    final Method timeMethod;
    final Options options;
    final WorkerEventLog log;

    Trial(Benchmark benchmark, String methodName, Options options, WorkerEventLog log)
        throws Exception {
      this.benchmark = benchmark;

      // where's the right place for 'time' to be prepended again?
      this.timeMethod = benchmark.getClass().getDeclaredMethod("time" + methodName, int.class);
      this.options = options;
      this.log = log;

      timeMethod.setAccessible(true);
    }

    long warmUp(int warmupReps) throws Exception {
      log.notifyWarmupPhaseStarting();

      long warmupNanos = invokeTimeMethod(warmupReps);
      int estimatedReps =
          (int) ((((double) warmupReps) / warmupNanos) * options.timingIntervalNanos);

      logger.fine(String.format(
          "performed %d reps in %dns. estimated %d reps for a %dns timing interval",
              warmupReps, warmupNanos, estimatedReps, options.timingIntervalNanos));

      return warmupNanos;
    }

    /**
     * Returns a random number of reps based on a normal distribution around the estimated number of
     * reps for the timing interval. The distribution used has a standard deviation of one fifth of
     * the estimated number of reps.
     */
    private int calculateTargetReps(long reps, long nanos) {
      double targetReps = (((double) reps) / nanos) * options.timingIntervalNanos;
      return Math.max(1, (int) Math.round((random.nextGaussian() * (targetReps / 5)) + targetReps));
    }

    void run(int warmupReps, long warmupNanos) throws Exception {
      log.notifyMeasurementPhaseStarting();

      long totalReps = warmupReps;
      long totalNanos = warmupNanos;

      while (true) {
        int reps = calculateTargetReps(totalReps, totalNanos);

        if (options.gcBeforeEach) {
          Util.forceGc();
        }

        // build as much as we can outside of timing
        Builder measurementBuilder = new Measurement.Builder()
            .description("runtime");

        log.notifyMeasurementStarting();
        long nanos = invokeTimeMethod(reps);
        log.notifyMeasurementEnding(measurementBuilder
            .value(Value.create(nanos, "ns"))
            .weight(reps)
            .build());

        totalReps += reps;
        totalNanos += nanos;
      }
    }

    private long invokeTimeMethod(int reps) throws Exception {
      long before = System.nanoTime();
      timeMethod.invoke(benchmark, reps);
      return System.nanoTime() - before;
    }
  }

  private static class Options {
    long timingIntervalNanos;
    boolean gcBeforeEach;

    Options(Map<String, String> optionMap) {
      this.timingIntervalNanos = Long.parseLong(optionMap.get("timingIntervalNanos"));
      this.gcBeforeEach = Boolean.parseBoolean(optionMap.get("gcBeforeEach"));
    }
  }

}
