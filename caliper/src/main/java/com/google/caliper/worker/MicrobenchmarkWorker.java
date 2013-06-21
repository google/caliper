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

import static java.util.concurrent.TimeUnit.NANOSECONDS;

import com.google.caliper.model.Measurement;
import com.google.caliper.model.Measurement.Builder;
import com.google.caliper.model.Value;
import com.google.caliper.runner.InvalidBenchmarkException;
import com.google.caliper.util.ShortDuration;
import com.google.caliper.util.Util;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Ticker;
import com.google.common.collect.Iterables;
import com.google.inject.Inject;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Map;
import java.util.Random;

// TODO(gak): split this into separate workers for microbenchmarks and picobenchmarks
public class MicrobenchmarkWorker implements Worker {
  @VisibleForTesting static final int INITIAL_REPS = 100;

  private final Random random;
  private final Ticker ticker;

  @Inject MicrobenchmarkWorker(Random random, Ticker ticker) {
    this.random = random;
    // TODO(gak): investigate whether or not we can use Stopwatch
    this.ticker = ticker;
  }

  @Override public void measure(Object benchmark, Method method,
      Map<String, String> optionMap, WorkerEventLog log) throws Exception {
    Options options = new Options(optionMap);
    Trial trial = createTrial(benchmark, method, options, log);
    long warmupNanos = trial.warmUp(INITIAL_REPS);
    trial.run(INITIAL_REPS, warmupNanos);
  }

  private Trial createTrial(Object benchmark, final Method method,
      Options options, WorkerEventLog log) {
    Class<?> repsType = Iterables.getOnlyElement(Arrays.asList(method.getParameterTypes()));
    if (int.class.equals(repsType)) {
      return new IntTrial(benchmark, method, options, log);
    } else if (long.class.equals(repsType)) {
      return new LongTrial(benchmark, method, options, log);
    } else {
      throw new IllegalStateException(String.format(
          "Got a benchmark method (%s) with an invalid reps parameter.", method));
    }
  }

  /**
   * Returns a random number of reps based on a normal distribution around the estimated number of
   * reps for the timing interval. The distribution used has a standard deviation of one fifth of
   * the estimated number of reps.
   */
  @VisibleForTesting static long calculateTargetReps(long reps, long nanos, long targetNanos,
      double gaussian) {
    double targetReps = (((double) reps) / nanos) * targetNanos;
    return Math.max(1L, Math.round((gaussian * (targetReps / 5)) + targetReps));
  }

  private abstract class Trial {
    final Object benchmark;
    final Method timeMethod;
    final Options options;
    final WorkerEventLog log;

    Trial(Object benchmark, Method timeMethod, Options options, WorkerEventLog log) {
      this.benchmark = benchmark;
      this.timeMethod = timeMethod;
      this.options = options;
      this.log = log;
    }

    long warmUp(int warmupReps) throws Exception {
      log.notifyWarmupPhaseStarting();
      return invokeTimeMethod(warmupReps);
    }

    void run(int warmupReps, long warmupNanos) throws Exception {
      log.notifyMeasurementPhaseStarting();

      long totalReps = warmupReps;
      long totalNanos = warmupNanos;

      while (true) {
        long reps = calculateTargetReps(totalReps, totalNanos, options.timingIntervalNanos,
            random.nextGaussian());

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

    abstract long invokeTimeMethod(long reps) throws Exception;
  }

  private final class IntTrial extends Trial {
    IntTrial(Object benchmark, Method timeMethod, Options options, WorkerEventLog log) {
      super(benchmark, timeMethod, options, log);
    }

    @Override  long invokeTimeMethod(long reps) throws Exception {
      int intReps = (int) reps;
      if (reps != intReps) {
        throw new InvalidBenchmarkException("%s.%s takes an int for reps, "
            + "but requires a greater number to fill the given timing interval (%s). "
            + "If this is expected (the benchmarked code is very fast), use a long parameter."
            + "Otherwise, check your benchmark for errors.",
                benchmark.getClass(), timeMethod.getName(),
                    ShortDuration.of(options.timingIntervalNanos, NANOSECONDS));
      }
      long before = ticker.read();
      timeMethod.invoke(benchmark, intReps);
      return ticker.read() - before;
    }
  }

  private final class LongTrial extends Trial {
    LongTrial(Object benchmark, Method timeMethod, Options options, WorkerEventLog log) {
      super(benchmark, timeMethod, options, log);
    }

    @Override long invokeTimeMethod(long reps) throws Exception {
      long before = ticker.read();
      timeMethod.invoke(benchmark, reps);
      return ticker.read() - before;
    }
  }

  private static final class Options {
    long timingIntervalNanos;
    boolean gcBeforeEach;

    Options(Map<String, String> optionMap) {
      this.timingIntervalNanos = Long.parseLong(optionMap.get("timingIntervalNanos"));
      this.gcBeforeEach = Boolean.parseBoolean(optionMap.get("gcBeforeEach"));
    }
  }

}
