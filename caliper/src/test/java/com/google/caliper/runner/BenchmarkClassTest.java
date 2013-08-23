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
import static org.junit.Assert.fail;

import com.google.caliper.AfterExperiment;
import com.google.caliper.BeforeExperiment;
import com.google.common.collect.ImmutableSet;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/**
 * Tests {@link BenchmarkClass}.
 */
@RunWith(JUnit4.class)
public class BenchmarkClassTest {
  @Test public void beforeMeasurementMethods_AnnotatedBenchmark() throws Exception {
    assertEquals(
        ImmutableSet.of(
            MyBenchmark.class.getDeclaredMethod("before1"),
            MyBenchmark.class.getDeclaredMethod("before2")),
        BenchmarkClass.forClass(MyBenchmark.class).beforeExperimentMethods());
  }

  @SuppressWarnings("deprecation") // tests for deprecated code
  @Test public void beforeMeasurementMethods_LegacyBenchmark() throws Exception {
    assertEquals(
        ImmutableSet.of(com.google.caliper.legacy.Benchmark.class.getDeclaredMethod("setUp")),
        BenchmarkClass.forClass(MyLegacyBenchmark.class).beforeExperimentMethods());
  }

  @SuppressWarnings("deprecation") // tests for deprecated code
  @Test public void afterMeasurementMethods_LegacyBenchmark() throws Exception {
    assertEquals(
        ImmutableSet.of(com.google.caliper.legacy.Benchmark.class.getDeclaredMethod("tearDown")),
        BenchmarkClass.forClass(MyLegacyBenchmark.class).afterExperimentMethods());
  }

  @Test public void afterMeasurementMethods_AnnotatedBenchmark() throws Exception {
    assertEquals(
        ImmutableSet.of(
            MyBenchmark.class.getDeclaredMethod("after1"),
            MyBenchmark.class.getDeclaredMethod("after2")),
        BenchmarkClass.forClass(MyBenchmark.class).afterExperimentMethods());
  }

  @Test public void forClass_inheritenceThrows() throws Exception {
    try {
      BenchmarkClass.forClass(MalformedBenhcmark.class);
      fail();
    } catch (InvalidBenchmarkException expected) {}
    try {
      BenchmarkClass.forClass(MalformedLegacyBenchmark.class);
      fail();
    } catch (InvalidBenchmarkException expected) {}
  }

  static class MyBenchmark {
    @BeforeExperiment void before1() {}
    @BeforeExperiment void before2() {}
    @AfterExperiment void after1() {}
    @AfterExperiment void after2() {}
  }

  static class MalformedBenhcmark extends MyBenchmark {}

  @SuppressWarnings("deprecation") // tests for deprecated code
  static class MyLegacyBenchmark extends com.google.caliper.legacy.Benchmark {}

  static class MalformedLegacyBenchmark extends MyLegacyBenchmark {}
}
