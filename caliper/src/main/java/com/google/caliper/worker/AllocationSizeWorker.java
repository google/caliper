// Copyright 2011 Google Inc. All Rights Reserved.

package com.google.caliper.worker;

import com.google.caliper.model.Measurement;

/**
 * {@link AllocationWorker} subclass that builds its {@link Measurement} based on the
 * total number of bytes of allocations observed.
 */
public class AllocationSizeWorker extends AllocationWorker {
  @Override Measurement extractMeasurement(AllocationStats stats, int reps) {
    Measurement measurement = new Measurement();
    measurement.value = stats.allocationSize;
    measurement.weight = reps;
    measurement.unit = "bytes";
    measurement.description = "bytes allocated";
    return measurement;
  }
}
