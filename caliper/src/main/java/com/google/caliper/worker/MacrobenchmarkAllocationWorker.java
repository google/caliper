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

import com.google.caliper.model.Measurement;
import com.google.caliper.runner.Running.Benchmark;
import com.google.caliper.runner.Running.BenchmarkMethod;
import com.google.common.collect.ImmutableList;

import java.lang.reflect.Method;

import javax.inject.Inject;

/**
 * The {@link Worker} for the {@code AllocationInstrument}.  This class invokes the benchmark method
 * a few times, with varying numbers of reps, and computes the number of object allocations and the
 * total size of those allocations.
 */
public final class MacrobenchmarkAllocationWorker extends Worker {
  private final AllocationRecorder recorder;

  @Inject MacrobenchmarkAllocationWorker(@Benchmark Object benchmark, 
      @BenchmarkMethod Method method, AllocationRecorder recorder) {
    super(benchmark, method);
    this.recorder = recorder;
  }

  @Override public void bootstrap() throws Exception {
    // do one initial measurement and throw away its results
    measureAllocations(benchmark, benchmarkMethod);
  }
  
  @Override public ImmutableList<Measurement> measure() throws Exception {
    return measureAllocations(benchmark, benchmarkMethod).toMeasurements();
  }

  private AllocationStats measureAllocations(Object benchmark, Method method) throws Exception {
    recorder.startRecording();
    method.invoke(benchmark);
    return recorder.stopRecording(1);
  }
}
