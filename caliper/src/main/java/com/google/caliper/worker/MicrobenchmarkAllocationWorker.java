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

import com.google.inject.Inject;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Map;
import java.util.Random;

/**
 * The {@link Worker} for the {@code AllocationInstrument}.  This class invokes the benchmark method
 * a few times, with varying numbers of reps, and computes the number of object allocations and the
 * total size of those allocations.
 */
public final class MicrobenchmarkAllocationWorker implements Worker {
  private static final int MAX_BASELINE_REPS = 5;
  private static final int MAX_REPS_ABOVE_BASELINE = 100;

  private final Random random;
  private final RecordingAllocationSampler recorder;

  @Inject MicrobenchmarkAllocationWorker(RecordingAllocationSampler recorder, Random random) {
    this.random = random;
    this.recorder = recorder;
  }

  @Override public void measure(Object benchmark, String methodName,
      Map<String, String> options, WorkerEventLog log) throws Exception {
    // do one initial measurement and throw away its results
    log.notifyWarmupPhaseStarting();
    measureAllocations(benchmark, methodName, 1);

    log.notifyMeasurementPhaseStarting();
    while (true) {
      log.notifyMeasurementStarting();
      // [1, 5]
      int baselineReps = random.nextInt(MAX_BASELINE_REPS) + 1;
      AllocationStats baseline = measureAllocations(benchmark, methodName, baselineReps);
      // (baseline, baseline + MAX_REPS_ABOVE_BASELINE]
      int measurementReps = baselineReps + random.nextInt(MAX_REPS_ABOVE_BASELINE) + 1;
      AllocationStats measurement = measureAllocations(benchmark, methodName, measurementReps);
      try {
        AllocationStats diff = measurement.minus(baseline);
        log.notifyMeasurementEnding(diff.toMeasurements());
      } catch (IllegalStateException e) {
        // log the failure, but just keep trying to measure
        log.notifyMeasurementFailure(e);
      }
    }
  }

  static Method findMethod(Class<?> benchmarkClass, String name) {
    for (Method method : benchmarkClass.getDeclaredMethods()) {
      Class<?>[] parameterTypes = method.getParameterTypes();
      if (method.getName().equals(name)
          && ((parameterTypes.length == 0)
              || Arrays.equals(parameterTypes, new Class<?>[] {int.class})
              || Arrays.equals(parameterTypes, new Class<?>[] {long.class}))) {
        method.setAccessible(true);
        return method;
      }
    }
    throw new IllegalArgumentException(
        String.format("no method named %s on %s that takes either an int or a long",
            name, benchmarkClass));
  }

  private AllocationStats measureAllocations(
      Object benchmark, String methodName, int reps) throws Exception {
    Method method = findMethod(benchmark.getClass(), methodName);
    // do the Integer boxing and the creation of the Object[] outside of the record block, so that
    // our internal allocations aren't counted in the benchmark's allocations.
    Object[] args = {reps};
    recorder.startRecording();
    method.invoke(benchmark, args);
    return recorder.stopRecording(reps);
  }
}
