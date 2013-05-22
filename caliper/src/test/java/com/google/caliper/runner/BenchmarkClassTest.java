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

import com.google.common.collect.ImmutableSet;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/**
 * Tests {@link BenchmarkClass}.
 */
@RunWith(JUnit4.class)
public class BenchmarkClassTest {
  @Test public void beforeMeasurementMethods_Benchmark() throws Exception {
    assertEquals(
        ImmutableSet.of(com.google.caliper.Benchmark.class.getDeclaredMethod("setUp")),
        BenchmarkClass.forClass(MyBenchmark.class).beforeMeasurementMethods());
  }

  @Test public void beforeMeasurementMethods_LegacyBenchmark() throws Exception {
    assertEquals(
        ImmutableSet.of(com.google.caliper.legacy.Benchmark.class.getDeclaredMethod("setUp")),
        BenchmarkClass.forClass(MyLegacyBenchmark.class).beforeMeasurementMethods());
  }

  @Test public void afterMeasurementMethods_Benchmark() throws Exception {
    assertEquals(
        ImmutableSet.of(com.google.caliper.Benchmark.class.getDeclaredMethod("tearDown")),
        BenchmarkClass.forClass(MyBenchmark.class).afterMeasurementMethods());
  }

  @Test public void afterMeasurementMethods_LegacyBenchmark() throws Exception {
    assertEquals(
        ImmutableSet.of(com.google.caliper.legacy.Benchmark.class.getDeclaredMethod("tearDown")),
        BenchmarkClass.forClass(MyLegacyBenchmark.class).afterMeasurementMethods());
  }

  static class MyBenchmark extends com.google.caliper.Benchmark {}

  static class MyLegacyBenchmark extends com.google.caliper.legacy.Benchmark {}
}
