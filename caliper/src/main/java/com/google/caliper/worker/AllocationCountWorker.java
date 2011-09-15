// Copyright 2011 Google Inc. All Rights Reserved.

package com.google.caliper.worker;

import com.google.caliper.model.Measurement;

/**
 * {@link AllocationWorker} subclass that builds its {@link Measurement} based on the
 * number of allocations observed.
 *
 * @author schmoe@google.com (mike nonemacher)
 */
public class AllocationCountWorker extends AllocationWorker {
  @Override Measurement extractMeasurement(AllocationStats stats, int reps) {
    Measurement measurement = new Measurement();
    measurement.value = stats.allocationCount;
    measurement.weight = reps;
    measurement.unit = "instances";
    measurement.description = "objects allocated";
    return measurement;
  }
}
