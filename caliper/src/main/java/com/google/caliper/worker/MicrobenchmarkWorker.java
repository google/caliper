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

import com.google.caliper.api.Benchmark;
import com.google.caliper.util.LastNValues;
import com.google.caliper.util.Util;

import java.lang.reflect.Method;
import java.util.Collection;
import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;

public class MicrobenchmarkWorker implements Worker {
  @Override public Collection<Measurement> measure(Benchmark benchmark, String methodName,
      Map<String, String> optionMap, WorkerEventLog log) throws Exception {
    Options options = new Options(optionMap);
    Trial trial = new Trial(benchmark, methodName, options, log);

    int targetReps = trial.warmUp();

    // TODO: make the minimum configurable, default to maybe 1000?
    if (targetReps < 100) {
      throw new Exception("Too few reps"); // TODO: better exception
    }
    return trial.run(targetReps);
  }

  private static class Trial {
    final Benchmark benchmark;
    final Method timeMethod;
    final Options options;
    final WorkerEventLog log;
    final long startTick;

    Trial(Benchmark benchmark, String methodName, Options options, WorkerEventLog log)
        throws Exception {
      this.benchmark = benchmark;

      // where's the right place for 'time' to be prepended again?
      this.timeMethod = benchmark.getClass().getDeclaredMethod("time" + methodName, int.class);
      this.options = options;
      this.log = log;
      this.startTick = System.nanoTime(); // TODO: Ticker?

      timeMethod.setAccessible(true);
    }

    int warmUp() throws Exception {
      log.notifyWarmupPhaseStarting();

      int targetReps = 1; // whatever
      long timeToEndWarmup = startTick + options.warmupNanos - options.timingIntervalNanos;

      while (System.nanoTime() < timeToEndWarmup) {
        long nanos = invokeTimeMethod(targetReps);
        targetReps = adjustRepCount(targetReps, nanos, options.timingIntervalNanos);
      }
      return targetReps;
    }

    Collection<Measurement> run(int targetReps) throws Exception {
      // Use a variety of rep counts for measurements, so that things like concavity and
      // y-intercept might be observed later. Just cycle through them in order (why not?).
      int[] repCounts = {
          (int) (targetReps * 0.7),
          (int) (targetReps * 0.9),
          (int) (targetReps * 1.1),
          (int) (targetReps * 1.3)
      };

      long timeToStop = startTick + options.maxTotalRuntimeNanos - options.timingIntervalNanos;

      Queue<Measurement> measurements = new LinkedList<Measurement>();

      LastNValues recentValues = new LastNValues(options.reportedIntervals);
      int i = 0;

      log.notifyMeasurementPhaseStarting();

      while (System.nanoTime() < timeToStop) {
        int reps = repCounts[i++ % repCounts.length];

        if (options.gcBeforeEach) {
          Util.forceGc();
        }

        log.notifyMeasurementStarting();
        long nanos = invokeTimeMethod(reps);

        Measurement m = new Measurement(nanos, reps);
        log.notifyMeasurementEnding(m.nanosPerRep);

        measurements.add(m);
        if (measurements.size() > options.reportedIntervals) {
          measurements.remove();
        }
        recentValues.add(m.nanosPerRep);

        if (shouldShortCircuit(recentValues)) {
          break;
        }
      }

      return measurements;
    }

    private boolean shouldShortCircuit(LastNValues lastN) {
      return lastN.isFull() && lastN.normalizedStddev() < options.shortCircuitTolerance;
    }

    private static int adjustRepCount(int previousReps, long previousNanos, long targetNanos) {
      // TODO: friendly error on overflow?
      // Note the * could overflow 2^63, but only if you're being kinda insane...
      return (int) (previousReps * targetNanos / previousNanos);
    }

    private long invokeTimeMethod(int reps) throws Exception {
      long before = System.nanoTime();
      timeMethod.invoke(benchmark, reps);
      return System.nanoTime() - before;
    }
  }

  private static class Options {
    long warmupNanos;
    long timingIntervalNanos;
    int reportedIntervals;
    double shortCircuitTolerance;
    long maxTotalRuntimeNanos;
    boolean gcBeforeEach;

    Options(Map<String, String> optionMap) {
      this.warmupNanos = Long.parseLong(optionMap.get("warmupNanos"));
      this.timingIntervalNanos = Long.parseLong(optionMap.get("timingIntervalNanos"));
      this.reportedIntervals = Integer.parseInt(optionMap.get("reportedIntervals"));
      this.shortCircuitTolerance = Double.parseDouble(optionMap.get("shortCircuitTolerance"));
      this.maxTotalRuntimeNanos = Long.parseLong(optionMap.get("maxTotalRuntimeNanos"));
      this.gcBeforeEach = Boolean.parseBoolean(optionMap.get("gcBeforeEach"));

      if (warmupNanos + reportedIntervals * timingIntervalNanos > maxTotalRuntimeNanos) {
        throw new RuntimeException("maxTotalRuntime is too low"); // TODO
      }
    }
  }

}
