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

import static com.google.common.base.Preconditions.checkArgument;

import com.google.caliper.model.Measurement;
import com.google.caliper.model.Value;
import com.google.common.base.Objects;
import com.google.common.collect.ImmutableList;
import com.google.inject.Inject;
import com.google.monitoring.runtime.instrumentation.AllocationRecorder;
import com.google.monitoring.runtime.instrumentation.Sampler;

import java.lang.reflect.Method;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * The {@link Worker} for the {@code AllocationInstrument}.  This class invokes the benchmark method
 * a few times, with varying numbers of reps, and computes the number of object allocations and the
 * total size of those allocations.
 */
public final class MacrobenchmarkAllocationWorker implements Worker {
  private final AtomicInteger allocationCount = new AtomicInteger();
  private final AtomicLong allocationSize = new AtomicLong();
  private volatile boolean recordAllocations = false;

  @Inject MacrobenchmarkAllocationWorker() {
    AllocationRecorder.addSampler(
        new Sampler() {
          @Override
          public void sampleAllocation(int arrayCount, String desc, Object newObj, long size) {
            if (recordAllocations) {
              allocationCount.getAndIncrement();
              allocationSize.getAndAdd(size);
            }
          }
        });
  }

  @Override public void measure(Object benchmark, String methodName,
      Map<String, String> options, WorkerEventLog log) throws Exception {
    // do one initial measurement and throw away its results
    log.notifyWarmupPhaseStarting();
    measureAllocations(benchmark, methodName);
    log.notifyMeasurementPhaseStarting();
    while (true) {
      log.notifyMeasurementStarting();
      AllocationStats measurement = measureAllocations(benchmark, methodName);
      log.notifyMeasurementEnding(ImmutableList.of(
          new Measurement.Builder()
              .value(Value.create(measurement.allocationCount, ""))
              .description("objects")
              .weight(1)
              .build(),
          new Measurement.Builder()
              .value(Value.create(measurement.allocationSize, "B"))
              .weight(1)
              .description("bytes")
              .build()));
    }
  }

  private AllocationStats measureAllocations(Object benchmark, String methodName) throws Exception {
    Method method = benchmark.getClass().getDeclaredMethod(methodName);
    method.setAccessible(true);
    clearAccumulatedStats();
    // do the Integer boxing and the creation of the Object[] outside of the record block, so that
    // our internal allocations aren't counted in the benchmark's allocations.
    recordAllocations = true;
    method.invoke(benchmark);
    recordAllocations = false;

    return new AllocationStats(allocationCount.get(), allocationSize.get());
  }

  private void clearAccumulatedStats() {
    recordAllocations = false;
    allocationCount.set(0);
    allocationSize.set(0);
  }

  static class AllocationStats {
    final int allocationCount;
    final long allocationSize;

    AllocationStats(int allocationCount, long allocationSize) {
      checkArgument(allocationCount >= 0, "allocationCount (%s) was negative", allocationCount);
      this.allocationCount = allocationCount;
      checkArgument(allocationSize >= 0, "allocationSize (%s) was negative", allocationSize);
      this.allocationSize = allocationSize;
    }

    @Override public String toString() {
      return Objects.toStringHelper(this)
          .add("allocationCount", allocationCount)
          .add("allocationSize", allocationSize)
          .toString();
    }
  }
}
