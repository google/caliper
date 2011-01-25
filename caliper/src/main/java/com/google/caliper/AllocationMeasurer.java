/*
 * Copyright (C) 2010 Google Inc.
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

import com.google.caliper.UserException.NonConstantMemoryUsage;
import com.google.common.base.Supplier;
import com.google.monitoring.runtime.instrumentation.AllocationRecorder;
import com.google.monitoring.runtime.instrumentation.Sampler;

public abstract class AllocationMeasurer extends Measurer {

  protected static final int ALLOCATION_DISPLAY_THRESHOLD = 50;

  private boolean log;
  private long tempAllocationCount;
  private long allocationsToIgnore;
  private long numberOfAllocations;
  private long allocationCount;
  private long outOfThreadAllocationCount;
  private boolean recordAllocations;
  protected String type;

  protected AllocationMeasurer() {
    log = false;
    allocationsToIgnore = 0;
    numberOfAllocations = 0;
    allocationCount = 0;
    outOfThreadAllocationCount = 0;
    recordAllocations = false;

    final Thread allocatingThread = Thread.currentThread();
    AllocationRecorder.addSampler(new Sampler() {
      // allocated {@code newObj} of type {@code desc}, whose size is {@code size}.
      // if this was not an array, {@code count} is -1. If it was array, {@code count} is the
      // size of the array.
      @Override public void sampleAllocation(int count, String desc, Object newObj, long size) {
        if (recordAllocations) {
          if (Thread.currentThread().equals(allocatingThread)) {
            if (log) {
              logAllocation(count, desc, size);
            } else if (numberOfAllocations == 0) {
              log("see first run for list of allocations");
            }
            allocationCount = incrementAllocationCount(allocationCount, count, size);
            tempAllocationCount++;
            numberOfAllocations++;
          } else {
            outOfThreadAllocationCount = incrementAllocationCount(outOfThreadAllocationCount, count, size);
            numberOfAllocations++;
          }
        }
      }
    });
  }

  protected abstract long incrementAllocationCount(long orig, int count, long size);

  private void logAllocation(int count, String desc, long size) {
    if (numberOfAllocations >= allocationsToIgnore) {
      if (numberOfAllocations < ALLOCATION_DISPLAY_THRESHOLD + allocationsToIgnore) {
        log("allocating " + desc + (count == -1 ? "" : " array with " + count + " elements")
            + " with size " + size + " bytes");
      } else if (numberOfAllocations == ALLOCATION_DISPLAY_THRESHOLD + allocationsToIgnore) {
        log("...more allocations...");
      }
    }
  }

  @Override public MeasurementSet run(Supplier<ConfiguredBenchmark> testSupplier) throws Exception {

    // warm up, for some reason the very first time anything is measured, it will have a few more
    // allocations.
    measureAllocations(testSupplier.get(), 1, 0);

    // The "one" case serves as a base line. There may be caching, lazy loading, etc going on here.
    tempAllocationCount = 0; // count the number of times the sampler is called in one rep
    long one = measureAllocationsTotal(testSupplier.get(), 1);
    long oneAllocations = tempAllocationCount;

    // we expect that the delta between any two consecutive reps will be constant
    tempAllocationCount = 0; // count the number of times the sampler is called in two reps
    long two = measureAllocationsTotal(testSupplier.get(), 2);
    long twoAllocations = tempAllocationCount;
    long expectedDiff = two - one;
    // there is some overhead on the first call that we can ignore for the purposes of measurement
    long unitsToIgnore = one - expectedDiff;
    allocationsToIgnore = 2 * oneAllocations - twoAllocations;
    log("ignoring " + allocationsToIgnore + " allocation(s) per measurement as overhead");

    Measurement[] allocationMeasurements = new Measurement[4];
    log = true;
    allocationMeasurements[0] = measureAllocations(testSupplier.get(), 1, unitsToIgnore);
    log = false;
    for (int i = 1; i < allocationMeasurements.length; i++) {
      allocationMeasurements[i] =
          measureAllocations(testSupplier.get(), i + 1, unitsToIgnore);
      if (Math.round(allocationMeasurements[i].getRaw()) != expectedDiff) {
        throw new NonConstantMemoryUsage();
      }
    }

    // The above logic guarantees that all the measurements are equal, so we only need to return a
    // single measurement.
    allocationsToIgnore = 0;
    return new MeasurementSet(allocationMeasurements[0]);
  }

  private Measurement measureAllocations(ConfiguredBenchmark benchmark, int reps, long toIgnore)
      throws Exception {
    prepareForTest();
    log(LogConstants.MEASURED_SECTION_STARTING);
    resetAllocations();
    recordAllocations = true;
    benchmark.run(reps);
    recordAllocations = false;
    log(LogConstants.MEASURED_SECTION_DONE);
    long allocations = (allocationCount - toIgnore) / reps;
    long outOfThreadAllocations = outOfThreadAllocationCount;
    log(allocations + " " + type + "(s) allocated per rep");
    log(outOfThreadAllocations + " out of thread " + type + "(s) allocated in " + reps + " reps");
    benchmark.close();
    return getMeasurement(benchmark, allocations);
  }

  protected abstract Measurement getMeasurement(ConfiguredBenchmark benchmark, long allocations);

  private long measureAllocationsTotal(ConfiguredBenchmark benchmark, int reps)
      throws Exception {
    prepareForTest();
    log(LogConstants.MEASURED_SECTION_STARTING);
    resetAllocations();
    recordAllocations = true;
    benchmark.run(reps);
    recordAllocations = false;
    log(LogConstants.MEASURED_SECTION_DONE);
    long allocations = allocationCount;
    long outOfThreadAllocations = outOfThreadAllocationCount;
    log(allocations + " " + type + "(s) allocated in " + reps + " reps");
    if (outOfThreadAllocations > 0) {
      log(outOfThreadAllocations + " out of thread " + type + "(s) allocated in " + reps + " reps");
    }
    benchmark.close();
    return allocations;
  }

  private void resetAllocations() {
    allocationCount = 0;
    outOfThreadAllocationCount = 0;
    numberOfAllocations = 0;
  }
}
