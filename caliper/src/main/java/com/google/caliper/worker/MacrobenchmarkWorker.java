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

import static com.google.caliper.util.Reflection.getAnnotatedMethods;
import static java.util.concurrent.TimeUnit.NANOSECONDS;

import com.google.caliper.Benchmark;
import com.google.caliper.api.AfterRep;
import com.google.caliper.api.BeforeRep;
import com.google.caliper.model.Measurement;
import com.google.caliper.model.Value;
import com.google.common.base.Stopwatch;
import com.google.common.base.Ticker;
import com.google.common.collect.ImmutableSet;
import com.google.inject.Inject;

import java.lang.reflect.Method;
import java.util.Map;
import java.util.Random;

/**
 */
public class MacrobenchmarkWorker implements Worker {
  private final Stopwatch stopwatch;

  @Inject MacrobenchmarkWorker(Random random, Ticker ticker) {
    this.stopwatch = new Stopwatch(ticker);
  }

  @Override public void measure(Benchmark benchmark, String methodName,
      Map<String, String> optionMap, WorkerEventLog log) throws Exception {
    Method method = benchmark.getClass().getDeclaredMethod(methodName);
    method.setAccessible(true);
    ImmutableSet<Method> beforeRepMethods =
        getAnnotatedMethods(benchmark.getClass(), BeforeRep.class);
    ImmutableSet<Method> afterRepMethods =
        getAnnotatedMethods(benchmark.getClass(), AfterRep.class);
    log.notifyMeasurementPhaseStarting();
    while (true) {
      for (Method beforeRepMethod : beforeRepMethods) {
        beforeRepMethod.invoke(benchmark);
      }
      log.notifyMeasurementStarting();
      try {
        stopwatch.start();
        method.invoke(benchmark);
        long nanos = stopwatch.stop().elapsed(NANOSECONDS);
        stopwatch.reset();
        log.notifyMeasurementEnding(new Measurement.Builder()
            .description("runtime")
            .weight(1)
            .value(Value.create(nanos, "ns"))
            .build());
      } finally {
        for (Method afterRepMethod : afterRepMethods) {
          afterRepMethod.invoke(benchmark);
        }
      }
    }
  }
}
