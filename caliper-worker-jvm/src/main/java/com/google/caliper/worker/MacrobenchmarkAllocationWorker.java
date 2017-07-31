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

import static com.google.caliper.util.Reflection.getAnnotatedMethods;

import com.google.caliper.api.AfterRep;
import com.google.caliper.api.BeforeRep;
import com.google.caliper.core.Running.Benchmark;
import com.google.caliper.core.Running.BenchmarkMethod;
import com.google.caliper.model.Measurement;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import java.lang.reflect.Method;
import javax.inject.Inject;

/**
 * The {@link Worker} for the {@code AllocationInstrument}. This class invokes the benchmark method
 * a few times, with varying numbers of reps, and computes the number of object allocations and the
 * total size of those allocations.
 */
public final class MacrobenchmarkAllocationWorker extends Worker {
  private final AllocationRecorder recorder;
  private final ImmutableSet<Method> beforeRepMethods;
  private final ImmutableSet<Method> afterRepMethods;

  @Inject
  MacrobenchmarkAllocationWorker(
      @Benchmark Object benchmark, @BenchmarkMethod Method method, AllocationRecorder recorder) {
    super(benchmark, method);
    this.recorder = recorder;
    this.beforeRepMethods = getAnnotatedMethods(benchmark.getClass(), BeforeRep.class);
    this.afterRepMethods = getAnnotatedMethods(benchmark.getClass(), AfterRep.class);
  }

  @Override
  public void bootstrap() throws Exception {
    // do one initial measurement and throw away its results
    preMeasure(true);
    measureAllocations(benchmark, benchmarkMethod);
    postMeasure();
  }

  @Override
  public void preMeasure(boolean inWarmup) throws Exception {
    for (Method beforeRepMethod : beforeRepMethods) {
      beforeRepMethod.invoke(benchmark);
    }
  }

  @Override
  public ImmutableList<Measurement> measure() throws Exception {
    return measureAllocations(benchmark, benchmarkMethod).toMeasurements();
  }

  @Override
  public void postMeasure() throws Exception {
    for (Method afterRepMethod : afterRepMethods) {
      afterRepMethod.invoke(benchmark);
    }
  }

  private AllocationStats measureAllocations(Object benchmark, Method method) throws Exception {
    recorder.startRecording();
    // Pass null to avoid auto-allocation of a varargs array.
    method.invoke(benchmark, (Object[]) null);
    return recorder.stopRecording(1);
  }
}
