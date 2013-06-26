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

package com.google.caliper.runner;

import static java.util.concurrent.TimeUnit.NANOSECONDS;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import com.google.caliper.Benchmark;
import com.google.caliper.runner.Instrument.Instrumentation;
import com.google.caliper.util.ShortDuration;
import com.google.caliper.worker.MacrobenchmarkWorker;
import com.google.caliper.worker.RuntimeWorker;
import com.google.common.base.Function;
import com.google.common.base.Predicate;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableSet;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.reflect.Method;
import java.util.Arrays;

/**
 * Tests {@link RuntimeInstrument}.
 */
@RunWith(JUnit4.class)
public class RuntimeInstrumentTest {
  private RuntimeInstrument instrument;

  @Before public void createInstrument() {
    this.instrument = new RuntimeInstrument(ShortDuration.of(100, NANOSECONDS),
        new PrintWriter(new StringWriter()), new PrintWriter(new StringWriter()));
  }

  @Test public void isBenchmarkMethod() {
    assertEquals(
        ImmutableSet.of("macrobenchmark", "microbenchmark", "picobenchmark", "integerParam"),
        FluentIterable.from(Arrays.asList(RuntimeBenchmark.class.getDeclaredMethods()))
            .filter(new Predicate<Method>() {
              @Override public boolean apply(Method input) {
                return instrument.isBenchmarkMethod(input);
              }
            })
            .transform(new Function<Method, String>() {
              @Override public String apply(Method input) {
                return input.getName();
              }
            })
            .toSet());
  }

  @Test public void createInstrumentation_macrobenchmark() throws Exception {
    Method benchmarkMethod = RuntimeBenchmark.class.getDeclaredMethod("macrobenchmark");
    Instrumentation instrumentation = instrument.createInstrumentation(benchmarkMethod);
    assertEquals(benchmarkMethod, instrumentation.benchmarkMethod());
    assertEquals(instrument, instrumentation.instrument());
    assertEquals(MacrobenchmarkWorker.class, instrumentation.workerClass());
  }

  @Test public void createInstrumentation_microbenchmark() throws Exception {
    Method benchmarkMethod = RuntimeBenchmark.class.getDeclaredMethod("microbenchmark", int.class);
    Instrumentation instrumentation = instrument.createInstrumentation(benchmarkMethod);
    assertEquals(benchmarkMethod, instrumentation.benchmarkMethod());
    assertEquals(instrument, instrumentation.instrument());
    assertEquals(RuntimeWorker.Micro.class, instrumentation.workerClass());
  }

  @Test public void createInstrumentation_picobenchmark() throws Exception {
    Method benchmarkMethod = RuntimeBenchmark.class.getDeclaredMethod("picobenchmark", long.class);
    Instrumentation instrumentation = instrument.createInstrumentation(benchmarkMethod);
    assertEquals(benchmarkMethod, instrumentation.benchmarkMethod());
    assertEquals(instrument, instrumentation.instrument());
    assertEquals(RuntimeWorker.Pico.class, instrumentation.workerClass());
  }

  @Test public void createInstrumentation_badParam() throws Exception {
    Method benchmarkMethod =
        RuntimeBenchmark.class.getDeclaredMethod("integerParam", Integer.class);
    try {
      instrument.createInstrumentation(benchmarkMethod);
      fail();
    } catch (InvalidBenchmarkException expected) {}
  }

  @Test public void createInstrumentation_notAMacrobenchmark() throws Exception {
    Method benchmarkMethod = RuntimeBenchmark.class.getDeclaredMethod("notAMacrobenchmark");
    try {
      instrument.createInstrumentation(benchmarkMethod);
      fail();
    } catch (IllegalArgumentException expected) {}
  }

  @Test public void createInstrumentationnotAMicrobenchmark() throws Exception {
    Method benchmarkMethod =
        RuntimeBenchmark.class.getDeclaredMethod("notAMicrobenchmark", int.class);
    try {
      instrument.createInstrumentation(benchmarkMethod);
      fail();
    } catch (IllegalArgumentException expected) {}
  }

  @Test public void createInstrumentation_notAPicobenchmark() throws Exception {
    Method benchmarkMethod =
        RuntimeBenchmark.class.getDeclaredMethod("notAPicobenchmark", long.class);
    try {
      instrument.createInstrumentation(benchmarkMethod);
      fail();
    } catch (IllegalArgumentException expected) {}
  }

  @SuppressWarnings("unused")
  private static final class RuntimeBenchmark {
    @Benchmark void macrobenchmark() {}
    @Benchmark void microbenchmark(int reps) {}
    @Benchmark void picobenchmark(long reps) {}

    @Benchmark void integerParam(Integer oops) {}

    void notAMacrobenchmark() {}
    void notAMicrobenchmark(int reps) {}
    void notAPicobenchmark(long reps) {}
  }
}
