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
import static java.util.Arrays.asList;

import com.google.caliper.runner.Running;
import com.google.common.collect.ConcurrentHashMultiset;
import com.google.monitoring.runtime.instrumentation.Sampler;

import javax.inject.Inject;

/**
 * An {@link AllocationRecorder} that records every allocation and its location.
 * 
 * <p>This recorder is enabled via the {@code trackAllocations} worker option.
 */
final class AllAllocationsRecorder extends AllocationRecorder {
  private final Class<?> benchmarkClass;
  private final String benchmarkMethodName;
  private volatile boolean recording = false;
  private final ConcurrentHashMultiset<Allocation> allocations = ConcurrentHashMultiset.create();
  
  private final Sampler sampler = new Sampler() {
    @Override public void sampleAllocation(int arrayCount, String desc, Object newObj, 
        long size) {
      if (recording) {
        if (arrayCount != -1) {
          desc = desc + "[" + arrayCount + "]";
        }
        // The first item is this line, the second is in AllocationRecorder and the
        // one before that is the allocating line, so we start at index 2.
        // We want to grab all lines until we get into the benchmark method.
        StackTraceElement[] stackTrace = new Exception().getStackTrace();
        int startIndex = 2;
        int endIndex = 2;
        for (int i = startIndex; i < stackTrace.length; i++) {
          StackTraceElement element = stackTrace[i];
          if (element.getClassName().startsWith(
              AllAllocationsRecorder.class.getPackage().getName())) {
            // Don't track locations up into the worker code, or originating within the worker 
            // code.
            break;
          }
          endIndex = i;
          if (element.getClassName().equals(benchmarkClass.getName()) 
              && element.getMethodName().equals(benchmarkMethodName)) {
            // stop logging at the method under test
            break;
          }
        }
        allocations.add(
            new Allocation(desc, size, asList(stackTrace).subList(startIndex, endIndex + 1)));
      }
    }
  };
  
  @Inject AllAllocationsRecorder(@Running.BenchmarkClass Class<?> benchmarkClass, 
      @Running.BenchmarkMethod String benchmarkMethodName) {
    this.benchmarkClass = benchmarkClass;
    this.benchmarkMethodName = benchmarkMethodName;
    com.google.monitoring.runtime.instrumentation.AllocationRecorder.addSampler(sampler);
  }
  
  @Override protected void doStartRecording() {
    checkState(!recording, "startRecording called, but we were already recording.");
    allocations.clear();
    recording = true;
  }
  
  @Override public AllocationStats stopRecording(int reps) {
    checkState(recording, "stopRecording called, but we were not recording.");
    recording = false;
    return new AllocationStats(allocations, reps);
  }
}