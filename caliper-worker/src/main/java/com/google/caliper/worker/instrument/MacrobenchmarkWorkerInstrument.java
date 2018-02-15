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

package com.google.caliper.worker.instrument;

import static com.google.caliper.util.Reflection.getAnnotatedMethods;
import static java.util.concurrent.TimeUnit.NANOSECONDS;

import com.google.caliper.api.AfterRep;
import com.google.caliper.api.BeforeRep;
import com.google.caliper.core.Running.Benchmark;
import com.google.caliper.core.Running.BenchmarkMethod;
import com.google.caliper.model.Measurement;
import com.google.caliper.model.Value;
import com.google.caliper.util.Util;
import com.google.common.base.Stopwatch;
import com.google.common.base.Ticker;
import com.google.common.collect.ImmutableSet;
import java.lang.reflect.Method;
import java.util.Map;
import javax.inject.Inject;

/** The {@link WorkerInstrument} implementation for macrobenchmarks. */
public class MacrobenchmarkWorkerInstrument extends WorkerInstrument {
  private final Stopwatch stopwatch;
  private final ImmutableSet<Method> beforeRepMethods;
  private final ImmutableSet<Method> afterRepMethods;
  private final boolean gcBeforeEach;

  @Inject
  MacrobenchmarkWorkerInstrument(
      @Benchmark Object benchmark,
      @BenchmarkMethod Method method,
      Ticker ticker,
      @WorkerInstrument.Options Map<String, String> options) {
    super(benchmark, method);
    this.stopwatch = Stopwatch.createUnstarted(ticker);
    this.beforeRepMethods = getAnnotatedMethods(benchmark.getClass(), BeforeRep.class);
    this.afterRepMethods = getAnnotatedMethods(benchmark.getClass(), AfterRep.class);
    this.gcBeforeEach = Boolean.parseBoolean(options.get("gcBeforeEach"));
  }

  @Override
  public void preMeasure(boolean inWarmup) throws Exception {
    for (Method beforeRepMethod : beforeRepMethods) {
      beforeRepMethod.invoke(benchmark);
    }
    if (gcBeforeEach && !inWarmup) {
      Util.forceGc();
    }
  }

  @Override
  public void dryRun() throws Exception {
    preMeasure(true);
    benchmarkMethod.invoke(benchmark);
    postMeasure();
  }

  @Override
  public Iterable<Measurement> measure() throws Exception {
    stopwatch.start();
    benchmarkMethod.invoke(benchmark);
    long nanos = stopwatch.stop().elapsed(NANOSECONDS);
    stopwatch.reset();
    return ImmutableSet.of(
        new Measurement.Builder()
            .description("runtime")
            .weight(1)
            .value(Value.create(nanos, "ns"))
            .build());
  }

  @Override
  public void postMeasure() throws Exception {
    for (Method afterRepMethod : afterRepMethods) {
      afterRepMethod.invoke(benchmark);
    }
  }
}
