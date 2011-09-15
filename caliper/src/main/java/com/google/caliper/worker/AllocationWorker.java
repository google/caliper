// Copyright 2011 Google Inc. All Rights Reserved.

package com.google.caliper.worker;

import static com.google.common.base.Preconditions.checkArgument;

import com.google.caliper.api.Benchmark;
import com.google.caliper.model.Measurement;
import com.google.common.collect.ImmutableSet;
import com.google.monitoring.runtime.instrumentation.AllocationRecorder;
import com.google.monitoring.runtime.instrumentation.Sampler;

import java.lang.reflect.Method;
import java.util.Collection;
import java.util.Map;

/**
 * Abstract class that contains the substance of the memory-allocation workers. This class invokes
 * the benchmark method a few times, with varying numbers of reps, and computes the number of
 * object allocations and the total size of those allocations. It delegates to its subclasses to
 * create the {@link Measurement} based on these observations.
 *
 * @author schmoe@google.com (mike nonemacher)
 */
abstract class AllocationWorker implements Worker {
  private int allocationCount;
  private long allocationSize;
  private boolean recordAllocations = false;

  protected AllocationWorker() {
    AllocationRecorder.addSampler(
        new Sampler() {
          @Override
          public void sampleAllocation(int arrayCount, String desc, Object newObj, long size) {
            if (recordAllocations) {
              allocationCount++;
              allocationSize += size;
            }
          }
        });
  }

  @Override public synchronized Collection<Measurement> measure(Benchmark benchmark,
      String methodName, Map<String, String> options, WorkerEventLog log) throws Exception {

    // do one initial measurement and throw away its results
    log.notifyWarmupPhaseStarting();
    measureAllocations(benchmark, methodName, 1);

    log.notifyMeasurementPhaseStarting();
    log.notifyMeasurementStarting();
    Measurement baseline = measureAllocations(benchmark, methodName, 1);
    log.notifyMeasurementEnding(baseline.value / baseline.weight);
    log.notifyMeasurementStarting();
    Measurement twoReps = measureAllocations(benchmark, methodName, 2);
    log.notifyMeasurementEnding(twoReps.value / twoReps.weight);
    Measurement diff = diffMeasurements(baseline, twoReps);

    return ImmutableSet.of(diff);
  }

  /**
   * Computes and returns the difference between these two measurements. The {@code initial}
   * measurement must have a lower weight (fewer reps) than the {@code second} measurement.
   */
  private Measurement diffMeasurements(Measurement initial, Measurement second) {
    Measurement diff = new Measurement();
    diff.value = second.value - initial.value;
    diff.weight = second.weight - initial.weight;
    checkArgument(diff.weight > 0);
    diff.unit = second.unit;
    diff.description = second.description;
    return diff;
  }

  private synchronized Measurement measureAllocations(
      Benchmark benchmark, String methodName, int reps) throws Exception {

    Method method = benchmark.getClass().getDeclaredMethod("time" + methodName, int.class);
    clearAccumulatedStats();
    // do the Integer boxing and the creation of the Object[] outside of the record block, so that
    // our internal allocations aren't counted in the benchmark's allocations.
    Object[] args = {reps};
    recordAllocations = true;
    method.invoke(benchmark, args);
    recordAllocations = false;

    return extractMeasurement(getAccumulatedStats(), reps);
  }

  private synchronized void clearAccumulatedStats() {
    recordAllocations = false;
    allocationCount = 0;
    allocationSize = 0;
  }

  private synchronized AllocationStats getAccumulatedStats() {
    return new AllocationStats(allocationCount, allocationSize);
  }

  abstract Measurement extractMeasurement(AllocationStats stats, int reps);

  static class AllocationStats {
    final int allocationCount;
    final long allocationSize;

    AllocationStats(int allocationCount, long allocationSize) {
      this.allocationCount = allocationCount;
      this.allocationSize = allocationSize;
    }
  }
}
