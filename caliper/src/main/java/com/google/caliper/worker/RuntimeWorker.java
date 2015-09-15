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
import com.google.caliper.model.Value;
import com.google.caliper.runner.InvalidBenchmarkException;
import com.google.caliper.runner.Running.Benchmark;
import com.google.caliper.runner.Running.BenchmarkMethod;
import com.google.caliper.util.ShortDuration;
import com.google.caliper.util.Util;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Ticker;
import com.google.common.collect.ImmutableSet;

import java.lang.reflect.Method;
import java.util.Map;
import java.util.Random;

import javax.inject.Inject;

/**
 * A {@link Worker} base class for micro and pico benchmarks.
 */
public abstract class RuntimeWorker extends Worker {
  @VisibleForTesting static final int INITIAL_REPS = 100;

  protected final Random random;
  protected final Ticker ticker;
  protected final Options options;
  private long totalReps;
  private long totalNanos;
  private long nextReps;

  RuntimeWorker(Object benchmark, 
      Method method, Random random, Ticker ticker,
      Map<String, String> workerOptions) {
    super(benchmark, method);
    this.random = random;
    // TODO(gak): investigate whether or not we can use Stopwatch
    this.ticker = ticker;
    this.options = new Options(workerOptions);
  }
  
  @Override public void bootstrap() throws Exception {
    totalReps = INITIAL_REPS;
    totalNanos = invokeTimeMethod(INITIAL_REPS);
  }

  @Override public void preMeasure(boolean inWarmup) throws Exception {
    nextReps = calculateTargetReps(totalReps, totalNanos, options.timingIntervalNanos,
        random.nextGaussian());
    if (options.gcBeforeEach && !inWarmup) {
      Util.forceGc();
    }
  }
  
  @Override public Iterable<Measurement> measure() throws Exception {
    long nanos = invokeTimeMethod(nextReps);
    Measurement measurement = new Measurement.Builder()
        .description("runtime")
        .value(Value.create(nanos, "ns"))
        .weight(nextReps)
        .build();
    
    totalReps += nextReps;
    totalNanos += nanos;
    return ImmutableSet.of(measurement);
  }
  
  abstract long invokeTimeMethod(long reps) throws Exception;

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

  /**
   * A {@link Worker} for micro benchmarks.
   */
  public static final class Micro extends RuntimeWorker {
    @Inject Micro(@Benchmark Object benchmark, 
        @BenchmarkMethod Method method, Random random, Ticker ticker,
        @WorkerOptions Map<String, String> workerOptions) {
      super(benchmark, method, random, ticker, workerOptions);
    }

    @Override long invokeTimeMethod(long reps) throws Exception {
      int intReps = (int) reps;
      if (reps != intReps) {
        throw new InvalidBenchmarkException("%s.%s takes an int for reps, "
            + "but requires a greater number to fill the given timing interval (%s). "
            + "If this is expected (the benchmarked code is very fast), use a long parameter."
            + "Otherwise, check your benchmark for errors.",
                benchmark.getClass(), benchmarkMethod.getName(),
                    ShortDuration.of(options.timingIntervalNanos, NANOSECONDS));
      }
      long before = ticker.read();
      benchmarkMethod.invoke(benchmark, intReps);
      return ticker.read() - before;
    }
  }

  /**
   * A {@link Worker} for pico benchmarks.
   */
  public static final class Pico extends RuntimeWorker {
    @Inject Pico(@Benchmark Object benchmark, 
        @BenchmarkMethod Method method, Random random, Ticker ticker,
        @WorkerOptions Map<String, String> workerOptions) {
      super(benchmark, method, random, ticker, workerOptions);
    }
    
    @Override long invokeTimeMethod(long reps) throws Exception {
      long before = ticker.read();
      benchmarkMethod.invoke(benchmark, reps);
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
