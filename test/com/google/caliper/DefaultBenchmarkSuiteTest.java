/*
 * Copyright (C) 2009 Google Inc.
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

package com.google.caliper;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;

import junit.framework.TestCase;

import java.util.Arrays;
import java.util.Collection;

public class DefaultBenchmarkSuiteTest extends TestCase {

  public void testIntrospection() {
    SampleBenchmarkSuite suite = new SampleBenchmarkSuite();
    assertEquals(ImmutableSet.of("a", "b"), suite.parameterNames());
    assertEquals(ImmutableSet.of("1", "2", "3"), suite.parameterValues("a"));
    assertEquals(ImmutableSet.of("4"), suite.parameterValues("b"));
    assertEquals(ImmutableSet.of(
        SampleBenchmarkSuite.MultiplyBenchmark.class,
        SampleBenchmarkSuite.DivideBenchmark.class),
        suite.benchmarkClasses());

  }

  public void testCreateBenchmark() {
    SampleBenchmarkSuite originalSuite = new SampleBenchmarkSuite();
    Benchmark benchmark = originalSuite.createBenchmark(
        SampleBenchmarkSuite.MultiplyBenchmark.class,
        ImmutableMap.of("a", "2", "b", "4"));

    SampleBenchmarkSuite.MultiplyBenchmark multiplyBenchmark
        = (SampleBenchmarkSuite.MultiplyBenchmark) benchmark;

    SampleBenchmarkSuite multiplySuite = multiplyBenchmark.suite();
    assertNotSame(originalSuite, multiplySuite);
    assertEquals(2, multiplySuite.a);
    assertEquals(4, multiplySuite.b);
  }

  static class SampleBenchmarkSuite extends DefaultBenchmarkSuite {
    @Param int a;

    private static Collection<Integer> aValues = Arrays.asList(1, 2, 3);

    @Param int b;

    private static Collection<Integer> bValues() {
      return Arrays.asList(4);
    }

    class MultiplyBenchmark extends Benchmark {
      @Override public Object run(int trials) throws Exception {
        int result = 0;
        for (int i = 0; i < trials; i++) {
          result ^= a * b;
        }
        return result;
      }

      SampleBenchmarkSuite suite() {
        return SampleBenchmarkSuite.this;
      }
    }

    class DivideBenchmark extends Benchmark {
      @Override public Object run(int trials) throws Exception {
        int result = 0;
        for (int i = 0; i < trials; i++) {
          result ^= a / b;
        }
        return result;
      }
    }
  }
}
