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

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.when;

import com.google.caliper.legacy.Benchmark;
import com.google.caliper.options.CaliperOptions;
import com.google.caliper.worker.Worker;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableSetMultimap;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.lang.reflect.Method;

/**
 * Tests {@link ExperimentingRunnerModule}.
 */
@RunWith(MockitoJUnitRunner.class)
public class ExperimentingRunnerModuleTest {
  private ExperimentingRunnerModule module = new ExperimentingRunnerModule();
  private Instrument instrumentA = new FakeInstrument();
  private Instrument instrumentB = new FakeInstrument();

  @Mock CaliperOptions options;

  private BenchmarkMethod methodA;
  private BenchmarkMethod methodB;
  private BenchmarkMethod methodC;

  @Before public void setUp() throws Exception {
    methodA = new BenchmarkMethod(BenchmarkClass.forClass(TestBenchmark.class),
        TestBenchmark.class.getDeclaredMethod("a"));
    methodB = new BenchmarkMethod(BenchmarkClass.forClass(TestBenchmark.class),
        TestBenchmark.class.getDeclaredMethod("b"));
    methodC = new BenchmarkMethod(BenchmarkClass.forClass(TestBenchmark.class),
        TestBenchmark.class.getDeclaredMethod("c"));
  }

  @Test public void provideBenchmarkMethodsByInstrument_noNames() throws Exception {
    when(options.benchmarkMethodNames()).thenReturn(ImmutableSet.<String>of());
    assertEquals(
        new ImmutableSetMultimap.Builder<Instrument, BenchmarkMethod>()
            .putAll(instrumentA, methodA, methodB, methodC)
            .putAll(instrumentB, methodA, methodB, methodC)
            .build(),
        module.provideBenchmarkMethodsByInstrument(options,
            BenchmarkClass.forClass(TestBenchmark.class),
            ImmutableSet.of(instrumentA, instrumentB)));
  }

  @SuppressWarnings("unchecked")
  @Test public void provideBenchmarkMethodsByInstrument_withNames() throws Exception {
    when(options.benchmarkMethodNames()).thenReturn(ImmutableSet.of("b"),
        ImmutableSet.of("a", "c"));
    assertEquals(
        new ImmutableSetMultimap.Builder<Instrument, BenchmarkMethod>()
            .putAll(instrumentA, methodB)
            .putAll(instrumentB, methodB)
            .build(),
        module.provideBenchmarkMethodsByInstrument(options,
            BenchmarkClass.forClass(TestBenchmark.class),
            ImmutableSet.of(instrumentA, instrumentB)));
    assertEquals(
        new ImmutableSetMultimap.Builder<Instrument, BenchmarkMethod>()
            .putAll(instrumentA, methodA, methodC)
            .putAll(instrumentB, methodA, methodC)
            .build(),
        module.provideBenchmarkMethodsByInstrument(options,
            BenchmarkClass.forClass(TestBenchmark.class),
            ImmutableSet.of(instrumentA, instrumentB)));
  }

  static final class TestBenchmark extends Benchmark {
    void a() {}
    void b() {}
    void c() {}
  }

  static final class FakeInstrument extends Instrument {
    @Override public boolean isBenchmarkMethod(Method method) {
      return true;
    }

    @Override
    public BenchmarkMethod createBenchmarkMethod(BenchmarkClass benchmarkClass, Method method) {
      return new BenchmarkMethod(benchmarkClass, method);
    }

    @Override
    public void dryRun(Object benchmark, BenchmarkMethod method) {}

    @Override public Class<? extends Worker> workerClass() {
      throw new UnsupportedOperationException();
    }

    @Override MeasurementCollectingVisitor getMeasurementCollectingVisitor() {
      throw new UnsupportedOperationException();
    }
  }
}
