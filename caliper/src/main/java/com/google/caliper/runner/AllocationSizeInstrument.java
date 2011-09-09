// Copyright 2011 Google Inc. All Rights Reserved.

package com.google.caliper.runner;

import com.google.caliper.worker.AllocationSizeWorker;
import com.google.caliper.worker.Worker;

/**
 * {@link AllocationInstrument} that measures the total number of bytes allocated by the
 * benchmark method.
 *
 * @author schmoe@google.com (mike nonemacher)
 */
public class AllocationSizeInstrument extends AllocationInstrument {
  @Override public Class<? extends Worker> workerClass() {
    return AllocationSizeWorker.class;
  }
}
