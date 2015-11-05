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
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import com.google.caliper.Benchmark;
import com.google.caliper.api.BeforeRep;
import com.google.caliper.api.Macrobenchmark;
import com.google.caliper.runner.Instrument.Instrumentation;
import com.google.caliper.util.ShortDuration;
import com.google.caliper.worker.MacrobenchmarkWorker;
import com.google.caliper.worker.RuntimeWorker;
import com.google.common.base.Function;
import com.google.common.base.Predicate;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableSet;
import com.google.common.util.concurrent.Uninterruptibles;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.concurrent.TimeUnit;

/**
 * Tests {@link RuntimeInstrument}.
 */
@RunWith(JUnit4.class)
public class RuntimeInstrumentTest {
  @Rule public CaliperTestWatcher runner = new CaliperTestWatcher();

  private RuntimeInstrument instrument;

  @Before public void createInstrument() {
    this.instrument = new RuntimeInstrument(ShortDuration.of(100, NANOSECONDS));
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

  private double relativeDifference(double a, double b) {
    return Math.abs(a - b) / ((a + b) / 2.0);
  }

  static final class TestBenchmark {
    @Benchmark long pico(long reps) {
      long dummy = 0;
      for (long i = 0; i < reps; i++) {
        dummy += spin();
      }
      return dummy;
    }

    @Benchmark long micro(int reps) {
      long dummy = 0;
      for (int i = 0; i < reps; i++) {
        dummy += spin();
      }
      return dummy;
    }

    @Macrobenchmark long macro() {
      return spin();
    }
  }

  // busy spin for 10ms and return the elapsed time.  N.B. we busy spin instead of sleeping so
  // that we aren't put at the mercy (and variance) of the thread scheduler.
  private static long spin() {
    long remainingNanos = TimeUnit.MILLISECONDS.toNanos(10);
    long start = System.nanoTime();
    long elapsed;
    while ((elapsed = System.nanoTime() - start) < remainingNanos) {}
    return elapsed;
  }

  @Test

  public void gcBeforeEachOptionIsHonored() throws Exception {
    runBenchmarkWithKnownHeap(true);
    // The GC error will only be avoided if gcBeforeEach is true, and
    // honored by the MacrobenchmarkWorker.
    assertFalse("No GC warning should be printed to stderr",
        runner.getStdout().toString().contains("WARNING: GC occurred during timing."));
  }

  @Test

  public void gcBeforeEachOptionIsReallyNecessary() throws Exception {
    // Verifies that we indeed get a GC warning if gcBeforeEach = false.
    runBenchmarkWithKnownHeap(false);
    assertTrue("A GC warning should be printed to stderr if gcBeforeEach isn't honored",
        runner.getStdout().toString().contains("WARNING: GC occurred during timing."));
  }

  private void runBenchmarkWithKnownHeap(boolean gcBeforeEach) throws Exception {
    runner.forBenchmark(BenchmarkThatAllocatesALot.class)
        .instrument("runtime")
        .options(
            "-Cvm.args=-Xmx512m",
            "-Cinstrument.runtime.options.measurements=10",
            "-Cinstrument.runtime.options.gcBeforeEach=" + gcBeforeEach,
            "--time-limit=30s")
        .run();
  }

  static final class BenchmarkThatAllocatesALot {
    @Benchmark
    int benchmarkMethod() {
      // Any larger and the GC doesn't manage to make enough space, resulting in
      // OOMErrors in both test cases above.
      long[] array = new long[32 * 1024 * 1024];
      return array.length;
    }
  }

  @Test

  public void maxWarmupWallTimeOptionIsHonored() throws Exception {
    runner.forBenchmark(MacroBenchmarkWithLongBeforeRep.class)
        .instrument("runtime")
        .options(
            "-Cinstrument.runtime.options.maxWarmupWallTime=100ms",
            "--time-limit=10s")
        .run();

    assertTrue(
        "The maxWarmupWallTime should trigger an interruption of warmup and a warning "
            + "should be printed to stderr",
        runner.getStdout().toString().contains(
            "WARNING: Warmup was interrupted "
                + "because it took longer than 100ms of wall-clock time."));
  }

  static final class MacroBenchmarkWithLongBeforeRep {
    @BeforeRep
    public void beforeRepMuchLongerThanBenchmark() {
      Uninterruptibles.sleepUninterruptibly(100, TimeUnit.MILLISECONDS);
    }

    @Benchmark
    long prettyFastMacroBenchmark() {
      return spin();
    }
  }
}
