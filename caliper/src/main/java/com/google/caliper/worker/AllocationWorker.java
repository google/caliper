// Copyright 2011 Google Inc. All Rights Reserved.

package com.google.caliper.worker;

import com.google.caliper.api.Benchmark;
import com.google.caliper.model.Measurement;
import com.google.common.collect.Lists;
import com.google.monitoring.runtime.instrumentation.AllocationRecorder;
import com.google.monitoring.runtime.instrumentation.Sampler;

import java.lang.reflect.Method;
import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * Abstract class that contains the substance of the memory-allocation workers. This class invokes
 * the benchmark method repeatedly, with varying numbers of reps, and computes the number of
 * object allocations and the total size of those allocations. It delegates to its subclasses to
 * create the {@link Measurement} based on these observations.
 *
 * @author schmoe@google.com (mike nonemacher)
 */
abstract class AllocationWorker implements Worker {
  private static final int[] REPS_PER_TRIAL = {1, 2, 4, 8, 16, 32, 64, 128, 256};

  private int mainThreadAllocationCount;
  private long mainThreadAllocationSize;
  private int otherThreadAllocationCount;
  private long otherThreadAllocationSize;
  private boolean recordAllocations = false;

  protected AllocationWorker() {
    AllocationRecorder.addSampler(
        new Sampler() {
          private final Thread mainThread = Thread.currentThread();

          @Override
          public void sampleAllocation(int arrayCount, String desc, Object newObj, long size) {
            if (recordAllocations) {
              if (Thread.currentThread() == mainThread) {
                mainThreadAllocationCount ++;
                mainThreadAllocationSize += size;
              } else {
                otherThreadAllocationCount ++;
                otherThreadAllocationSize += size;
              }
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
    final List<Measurement> measurements = Lists.newArrayList();
    for (int reps : REPS_PER_TRIAL) {
      log.notifyMeasurementStarting();
      Measurement measurement = measureAllocations(benchmark, methodName, reps);
      measurements.add(measurement);
      log.notifyMeasurementEnding(measurement.value / measurement.weight);
    }

    return measurements;
  }

  private synchronized Measurement measureAllocations(
      Benchmark benchmark, String methodName, int reps) throws Exception {

    Method method = benchmark.getClass().getDeclaredMethod(methodName, int.class);
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
    mainThreadAllocationCount = 0;
    mainThreadAllocationSize = 0;
    otherThreadAllocationCount = 0;
    otherThreadAllocationSize = 0;
  }

  private synchronized AllocationStats getAccumulatedStats() {
    return new AllocationStats(mainThreadAllocationCount, mainThreadAllocationSize,
        otherThreadAllocationCount, otherThreadAllocationSize);
  }

  abstract Measurement extractMeasurement(AllocationStats stats, int reps);

  static class AllocationStats {
    final int mainThreadAllocationCount;
    final long mainThreadAllocationSize;
    final int otherThreadsAllocationCount;
    final long otherThreadsAllocationSize;

    AllocationStats(int mainThreadAllocationCount, long mainThreadAllocationSize,
        int otherThreadsAllocationCount, long otherThreadsAllocationSize) {
      this.mainThreadAllocationCount = mainThreadAllocationCount;
      this.mainThreadAllocationSize = mainThreadAllocationSize;
      this.otherThreadsAllocationCount = otherThreadsAllocationCount;
      this.otherThreadsAllocationSize = otherThreadsAllocationSize;
    }
  }
}
