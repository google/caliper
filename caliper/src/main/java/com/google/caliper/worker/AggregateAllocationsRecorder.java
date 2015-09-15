/*
 * Copyright (C) 2013 Google Inc.
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

import static com.google.common.base.Preconditions.checkState;

import com.google.monitoring.runtime.instrumentation.Sampler;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import javax.inject.Inject;

/**
 * An {@link AllocationRecorder} that records the number and cumulative size of allocation.
 */
final class AggregateAllocationsRecorder extends AllocationRecorder {
  private final AtomicInteger allocationCount = new AtomicInteger();
  private final AtomicLong allocationSize = new AtomicLong();
  private volatile boolean recording = false;
  
  private final Sampler sampler = new Sampler() {
    @Override public void sampleAllocation(int arrayCount, String desc, Object newObj, 
        long size) {
      if (recording) {
        allocationCount.getAndIncrement();
        allocationSize.getAndAdd(size);
      }
    }
  };
  
  @Inject AggregateAllocationsRecorder() {
    com.google.monitoring.runtime.instrumentation.AllocationRecorder.addSampler(sampler);
  }
  
  @Override protected void doStartRecording() {
    checkState(!recording, "startRecording called, but we were already recording.");
    allocationCount.set(0);
    allocationSize.set(0);
    recording = true;
  }
  
  @Override public AllocationStats stopRecording(int reps) {
    checkState(recording, "stopRecording called, but we were not recording.");
    recording = false;
    return new AllocationStats(allocationCount.get(), allocationSize.get(), reps);
  }
}