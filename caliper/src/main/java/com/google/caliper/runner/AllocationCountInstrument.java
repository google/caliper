// Copyright 2011 Google Inc. All Rights Reserved.

package com.google.caliper.runner;

import com.google.caliper.worker.AllocationCountWorker;
import com.google.caliper.worker.Worker;

/**
 * {@link AllocationInstrument} that measures the number of objects allocated by the
 * benchmark method.
 */
public class AllocationCountInstrument extends AllocationInstrument {
  @Override public Class<? extends Worker> workerClass() {
    return AllocationCountWorker.class;
  }
}
