/*
 * Copyright (C) 2015 Google Inc.
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

import com.google.caliper.Param;
import com.google.common.collect.ImmutableSortedMap;
import junit.framework.TestCase;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/**
 * Test for {@link BenchmarkCreator}
 */
@RunWith(JUnit4.class)
public class BenchmarkCreatorTest extends TestCase {

  @Test
  public void publicDefaultConstructorNoParamBenchmark() {
    BenchmarkCreator creator = new BenchmarkCreator(PublicDefaultConstructorNoParamBenchmark.class,
        ImmutableSortedMap.<String, String>of());

    Object benchmarkInstance = creator.createBenchmarkInstance();
    assertTrue(benchmarkInstance instanceof PublicDefaultConstructorNoParamBenchmark);
  }

  public static class PublicDefaultConstructorNoParamBenchmark {
  }

  @Test
  public void publicDefaultConstructorWithParamBenchmark() {
    BenchmarkCreator creator = new BenchmarkCreator(
        PublicDefaultConstructorWithParamBenchmark.class,
        ImmutableSortedMap.of("byteField", "1", "intField", "2", "stringField", "string"));

    Object benchmarkInstance = creator.createBenchmarkInstance();
    assertTrue(benchmarkInstance instanceof PublicDefaultConstructorWithParamBenchmark);
    PublicDefaultConstructorWithParamBenchmark benchmark =
        (PublicDefaultConstructorWithParamBenchmark) benchmarkInstance;
    assertEquals(1, benchmark.byteField);
    assertEquals(2, benchmark.intField);
    assertEquals("string", benchmark.stringField);
  }

  public static class PublicDefaultConstructorWithParamBenchmark {
    @Param
    byte byteField;

    @Param
    int intField;

    @Param
    String stringField;
  }

  @Test
  public void publicNoSuitableConstructorBenchmark() {
    try {
      new BenchmarkCreator(
          PublicNoSuitableConstructorBenchmark.class,
          ImmutableSortedMap.<String, String>of());
    } catch (UserCodeException e) {
      assertEquals("Benchmark class "
          + PublicNoSuitableConstructorBenchmark.class.getName()
          + " does not have a publicly visible default constructor", e.getMessage());
    }
  }

  public static class PublicNoSuitableConstructorBenchmark {
    @Param
    byte byteField;

    @Param
    int intField;

    @Param
    String stringField;

    public PublicNoSuitableConstructorBenchmark(
        byte byteField, int intField, String stringField) {
      this.byteField = byteField;
      this.intField = intField;
      this.stringField = stringField;
    }
  }
}
