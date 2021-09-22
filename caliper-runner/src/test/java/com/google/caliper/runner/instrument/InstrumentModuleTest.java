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
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.when;

import com.google.caliper.Benchmark;
import com.google.caliper.core.BenchmarkClassModel;
import com.google.caliper.core.BenchmarkClassModel.MethodModel;
import com.google.caliper.model.InstrumentType;
import com.google.caliper.runner.config.SupportsVmType;
import com.google.caliper.runner.config.VmType;
import com.google.caliper.runner.instrument.Instrument.InstrumentedMethod;
import com.google.caliper.runner.options.CaliperOptions;
import com.google.common.collect.ImmutableSet;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

/** Tests {@link InstrumentModule}. */
@RunWith(MockitoJUnitRunner.class)
public class InstrumentModuleTest {

  private static final BenchmarkClassModel TEST_BENCHMARK_MODEL =
      BenchmarkClassModel.create(TestBenchmark.class);

  private Instrument instrumentA = new FakeInstrument();
  private Instrument instrumentB = new FakeInstrument();

  @Mock CaliperOptions options;

  private MethodModel methodA;
  private MethodModel methodB;
  private MethodModel methodC;

  @Before
  public void setUp() throws Exception {
    methodA = MethodModel.of(TestBenchmark.class.getDeclaredMethod("a"));
    methodB = MethodModel.of(TestBenchmark.class.getDeclaredMethod("b"));
    methodC = MethodModel.of(TestBenchmark.class.getDeclaredMethod("c"));
  }

  @Test
  public void provideInstrumentedMethods_noNames() throws Exception {
    when(options.benchmarkMethodNames()).thenReturn(ImmutableSet.<String>of());
    assertEquals(
        new ImmutableSet.Builder<InstrumentedMethod>()
            .add(instrumentA.createInstrumentedMethod(methodA))
            .add(instrumentA.createInstrumentedMethod(methodB))
            .add(instrumentA.createInstrumentedMethod(methodC))
            .add(instrumentB.createInstrumentedMethod(methodA))
            .add(instrumentB.createInstrumentedMethod(methodB))
            .add(instrumentB.createInstrumentedMethod(methodC))
            .build(),
        InstrumentModule.provideInstrumentedMethods(
            options, TEST_BENCHMARK_MODEL, ImmutableSet.of(instrumentA, instrumentB)));
  }

  @SuppressWarnings("unchecked")
  @Test
  public void provideInstrumentedMethods_withNames() throws Exception {
    when(options.benchmarkMethodNames())
        .thenReturn(ImmutableSet.of("b"), ImmutableSet.of("a", "c"));
    assertEquals(
        new ImmutableSet.Builder<InstrumentedMethod>()
            .add(instrumentA.createInstrumentedMethod(methodB))
            .add(instrumentB.createInstrumentedMethod(methodB))
            .build(),
        InstrumentModule.provideInstrumentedMethods(
            options, TEST_BENCHMARK_MODEL, ImmutableSet.of(instrumentA, instrumentB)));
    assertEquals(
        new ImmutableSet.Builder<InstrumentedMethod>()
            .add(instrumentA.createInstrumentedMethod(methodA))
            .add(instrumentA.createInstrumentedMethod(methodC))
            .add(instrumentB.createInstrumentedMethod(methodA))
            .add(instrumentB.createInstrumentedMethod(methodC))
            .build(),
        InstrumentModule.provideInstrumentedMethods(
            options, TEST_BENCHMARK_MODEL, ImmutableSet.of(instrumentA, instrumentB)));
  }

  @Test
  public void provideInstrumentedMethods_withInvalidName() {
    when(options.benchmarkMethodNames()).thenReturn(ImmutableSet.of("a", "c", "bad"));
    try {
      InstrumentModule.provideInstrumentedMethods(
          options, TEST_BENCHMARK_MODEL, ImmutableSet.of(instrumentA, instrumentB));
      fail("should have thrown for invalid benchmark method name");
    } catch (Exception expected) {
      assertThat(expected.getMessage()).contains("[bad]");
    }
  }

  static final class TestBenchmark {
    @Benchmark
    void a() {}

    @Benchmark
    void b() {}

    @Benchmark
    void c() {}
  }

  @SupportsVmType(VmType.JVM)
  static final class FakeInstrument extends Instrument {
    @Override
    public boolean isBenchmarkMethod(MethodModel method) {
      return true;
    }

    @Override
    public InstrumentedMethod createInstrumentedMethod(MethodModel benchmarkMethod) {
      return new InstrumentedMethod(benchmarkMethod) {
        @Override
        public InstrumentType type() {
          throw new UnsupportedOperationException();
        }

        @Override
        public MeasurementCollectingVisitor getMeasurementCollectingVisitor() {
          throw new UnsupportedOperationException();
        }
      };
    }
  }
}
