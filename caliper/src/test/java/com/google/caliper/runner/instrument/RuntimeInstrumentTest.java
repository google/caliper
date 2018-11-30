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

package com.google.caliper.runner.instrument;

import static com.google.common.truth.Truth.assertThat;
import static java.util.concurrent.TimeUnit.NANOSECONDS;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import com.google.caliper.Benchmark;
import com.google.caliper.api.BeforeRep;
import com.google.caliper.api.Macrobenchmark;
import com.google.caliper.core.BenchmarkClassModel.MethodModel;
import com.google.caliper.core.InvalidBenchmarkException;
import com.google.caliper.model.InstrumentType;
import com.google.caliper.model.Measurement;
import com.google.caliper.model.Trial;
import com.google.caliper.runner.instrument.Instrument.InstrumentedMethod;
import com.google.caliper.runner.testing.CaliperTestWatcher;
import com.google.caliper.util.ShortDuration;
import com.google.common.base.Function;
import com.google.common.base.Predicate;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.math.LinearTransformation;
import com.google.common.math.PairedStatsAccumulator;
import com.google.common.util.concurrent.Uninterruptibles;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.concurrent.TimeUnit;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/** Tests {@link RuntimeInstrument}. */
@RunWith(JUnit4.class)
public class RuntimeInstrumentTest {
  @Rule public final CaliperTestWatcher runner = new CaliperTestWatcher();

  private RuntimeInstrument instrument;

  @Before
  public void createInstrument() {
    this.instrument = new RuntimeInstrument(ShortDuration.of(100, NANOSECONDS));
  }

  @Test
  public void isBenchmarkMethod() {
    assertEquals(
        ImmutableSet.of("macrobenchmark", "microbenchmark", "picobenchmark", "integerParam"),
        FluentIterable.from(Arrays.asList(RuntimeBenchmark.class.getDeclaredMethods()))
            .transform(
                new Function<Method, MethodModel>() {
                  @Override
                  public MethodModel apply(Method input) {
                    return MethodModel.of(input);
                  }
                })
            .filter(
                new Predicate<MethodModel>() {
                  @Override
                  public boolean apply(MethodModel input) {
                    return instrument.isBenchmarkMethod(input);
                  }
                })
            .transform(
                new Function<MethodModel, String>() {
                  @Override
                  public String apply(MethodModel input) {
                    return input.name();
                  }
                })
            .toSet());
  }

  @Test
  public void createInstrumentedMethod_macrobenchmark() throws Exception {
    MethodModel benchmarkMethod = runtimeBenchmarkMethod("macrobenchmark");
    InstrumentedMethod instrumentedMethod = instrument.createInstrumentedMethod(benchmarkMethod);
    assertEquals(benchmarkMethod, instrumentedMethod.benchmarkMethod());
    assertEquals(instrument, instrumentedMethod.instrument());
    assertEquals(InstrumentType.RUNTIME_MACRO, instrumentedMethod.type());
  }

  @Test
  public void createInstrumentedMethod_microbenchmark() throws Exception {
    MethodModel benchmarkMethod = runtimeBenchmarkMethod("microbenchmark", int.class);
    InstrumentedMethod instrumentedMethod = instrument.createInstrumentedMethod(benchmarkMethod);
    assertEquals(benchmarkMethod, instrumentedMethod.benchmarkMethod());
    assertEquals(instrument, instrumentedMethod.instrument());
    assertEquals(InstrumentType.RUNTIME_MICRO, instrumentedMethod.type());
  }

  @Test
  public void createInstrumentedMethod_picobenchmark() throws Exception {
    MethodModel benchmarkMethod = runtimeBenchmarkMethod("picobenchmark", long.class);
    InstrumentedMethod instrumentedMethod = instrument.createInstrumentedMethod(benchmarkMethod);
    assertEquals(benchmarkMethod, instrumentedMethod.benchmarkMethod());
    assertEquals(instrument, instrumentedMethod.instrument());
    assertEquals(InstrumentType.RUNTIME_PICO, instrumentedMethod.type());
  }

  @Test
  public void createInstrumentedMethod_badParam() throws Exception {
    MethodModel benchmarkMethod = runtimeBenchmarkMethod("integerParam", Integer.class);
    try {
      instrument.createInstrumentedMethod(benchmarkMethod);
      fail();
    } catch (InvalidBenchmarkException expected) {
    }
  }

  @Test
  public void createInstrumentedMethod_notAMacrobenchmark() throws Exception {
    MethodModel benchmarkMethod = runtimeBenchmarkMethod("notAMacrobenchmark");
    try {
      instrument.createInstrumentedMethod(benchmarkMethod);
      fail();
    } catch (IllegalArgumentException expected) {
    }
  }

  @Test
  public void createInstrumentedMethodnotAMicrobenchmark() throws Exception {
    MethodModel benchmarkMethod = runtimeBenchmarkMethod("notAMicrobenchmark", int.class);
    try {
      instrument.createInstrumentedMethod(benchmarkMethod);
      fail();
    } catch (IllegalArgumentException expected) {
    }
  }

  @Test
  public void createInstrumentedMethod_notAPicobenchmark() throws Exception {
    MethodModel benchmarkMethod = runtimeBenchmarkMethod("notAPicobenchmark", long.class);
    try {
      instrument.createInstrumentedMethod(benchmarkMethod);
      fail();
    } catch (IllegalArgumentException expected) {
    }
  }

  private static MethodModel runtimeBenchmarkMethod(String name, Class<?>... parameterTypes)
      throws NoSuchMethodException {
    return MethodModel.of(RuntimeBenchmark.class.getDeclaredMethod(name, parameterTypes));
  }

  @SuppressWarnings("unused")
  private static final class RuntimeBenchmark {
    @Benchmark
    void macrobenchmark() {}

    @Benchmark
    void microbenchmark(int reps) {}

    @Benchmark
    void picobenchmark(long reps) {}

    @Benchmark
    void integerParam(Integer oops) {}

    void notAMacrobenchmark() {}

    void notAMicrobenchmark(int reps) {}

    void notAPicobenchmark(long reps) {}
  }

  @Ignore // very flaky
  @Test

  public void success() throws Exception {
    runner
        .forBenchmark(TestBenchmark.class)
        .instrument("runtime")
        .options(
            "-Cinstrument.runtime.options.warmup=10s",
            "-Cinstrument.runtime.options.timingInterval=100ms",
            "-Cinstrument.runtime.options.gcBeforeEach=false",
            "-Cinstrument.runtime.options.measurements=50",
            "--time-limit=30s")
        .run();
    double macroAverage = -1;
    double microAverage = -1;
    double picoAverage = -1;
    ImmutableList<Trial> trials = runner.trials();
    assertEquals("Expected 3 trials: " + trials, 3, trials.size());
    for (Trial trial : trials) {
      PairedStatsAccumulator stats = new PairedStatsAccumulator();
      for (Measurement measurement : trial.measurements()) {
        assertEquals("runtime", measurement.description());
        stats.add(measurement.weight(), measurement.value().magnitude());
      }
      LinearTransformation line = stats.leastSquaresFit();
      String methodName = trial.scenario().benchmarkSpec().methodName();
      if (line.isVertical()) {
        assertEquals("macro", methodName);
        // macro benchmark measurements all have a weight of 1 so the linear transformation is a
        // vertical line (no slope).
        macroAverage = stats.yStats().mean();
      } else {
        assertTrue("The slope should be positive, got " + line.slope(), line.slope() > 0.0);
        if ("pico".equals(methodName)) {
          picoAverage = line.slope();
        } else if ("micro".equals(methodName)) {
          microAverage = line.slope();
        } else {
          fail("unexpected method name: " + methodName);
        }
      }
    }
    assertTrue(
        "We should have seen results from all trials",
        macroAverage > 0 && picoAverage > 0 && microAverage > 0);
    // All the results should be the same within a margin of error of 50%. Since tests don't tend
    // to give very consistent performance results, lower margins of error are very flaky. This
    // could still be flaky, but should be less of the time at least, while it should still catch
    // any problems where results end up, say, an order of magnitude different.
    double marginOfError = 0.5;
    assertThat(relativeDifference(picoAverage, microAverage)).isLessThan(marginOfError);
    assertThat(relativeDifference(microAverage, macroAverage)).isLessThan(marginOfError);
    assertThat(relativeDifference(macroAverage, picoAverage)).isLessThan(marginOfError);
  }

  private double relativeDifference(double a, double b) {
    return Math.abs(a - b) / ((a + b) / 2.0);
  }

  static final class TestBenchmark {
    @Benchmark
    long pico(long reps) {
      long dummy = 0;
      for (long i = 0; i < reps; i++) {
        dummy += spin();
      }
      return dummy;
    }

    @Benchmark
    long micro(int reps) {
      long dummy = 0;
      for (int i = 0; i < reps; i++) {
        dummy += spin();
      }
      return dummy;
    }

    @Macrobenchmark
    long macro() {
      return spin();
    }
  }

  // busy spin for 5ms and return the elapsed time.  N.B. we busy spin instead of sleeping so
  // that we aren't put at the mercy (and variance) of the thread scheduler.
  private static long spin() {
    long remainingNanos = TimeUnit.MILLISECONDS.toNanos(5);
    long start = System.nanoTime();
    long elapsed;
    while ((elapsed = System.nanoTime() - start) < remainingNanos) {}
    return elapsed;
  }

  @Ignore // very flaky; no tweaks I've tried have fixed that
  @Test

  public void gcBeforeEachOptionIsHonored() throws Exception {
    runBenchmarkWithKnownHeap(true);
    // The GC error will only be avoided if gcBeforeEach is true, and
    // honored by the MacrobenchmarkWorker.
    assertFalse(
        "No GC warning should be printed to stderr",
        runner.getStdout().toString().contains("WARNING: GC occurred during timing."));
  }

  @Ignore // may also be flaky
  @Test

  public void gcBeforeEachOptionIsReallyNecessary() throws Exception {
    // Verifies that we indeed get a GC warning if gcBeforeEach = false.
    runBenchmarkWithKnownHeap(false);
    assertTrue(
        "A GC warning should be printed to stderr if gcBeforeEach isn't honored",
        runner.getStdout().toString().contains("WARNING: GC occurred during timing."));
  }

  private void runBenchmarkWithKnownHeap(boolean gcBeforeEach) throws Exception {
    runner
        .forBenchmark(BenchmarkThatAllocatesALot.class)
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
      // semi-arbitrary size that (ideally) should cause GC during timing when (and only when)
      // GC is *not* run between each timing run (gcBeforeEach=false)
      long[] array = new long[16 * 1024 * 1024];
      return array.length;
    }
  }

  @Test

  public void maxWarmupWallTimeOptionIsHonored() throws Exception {
    runner
        .forBenchmark(MacroBenchmarkWithLongBeforeRep.class)
        .instrument("runtime")
        .options("-Cinstrument.runtime.options.maxWarmupWallTime=100ms", "--time-limit=10s")
        .run();

    assertTrue(
        "The maxWarmupWallTime should trigger an interruption of warmup and a warning "
            + "should be printed to stderr",
        runner
            .getStdout()
            .toString()
            .contains(
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
