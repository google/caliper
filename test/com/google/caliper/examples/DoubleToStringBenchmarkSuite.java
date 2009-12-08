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

package com.google.caliper.examples;

import com.google.caliper.Benchmark;
import com.google.caliper.DefaultBenchmarkSuite;
import com.google.caliper.Param;
import com.google.caliper.Runner;

import java.util.Arrays;
import java.util.Collection;

/**
 * Measures the various ways the JDK converts doubles to Strings.
 */
public class DoubleToStringBenchmarkSuite extends DefaultBenchmarkSuite {

  @Param private Double d;

  private static Collection<Double> dValues = Arrays.asList(
      Math.PI,
      -0.0d,
      Double.NEGATIVE_INFINITY,
      Double.NaN
  );

  class FormatterBenchmark extends Benchmark {
    public Object run(int trials) {
      Double value = d;
      String result = null;
      for (int i = 0; i < trials; i++) {
        result = String.format("%f", value);
      }
      return result;
    }
  }

  class ToStringBenchmark extends Benchmark {
    public Object run(int trials) {
      Double value = d;
      String result = null;
      for (int i = 0; i < trials; i++) {
        result = value.toString();
      }
      return result;
    }
  }

  class ConcatenationBenchmark extends Benchmark {
    public Object run(int trials) {
      Double value = d;
      String result = null;
      for (int i = 0; i < trials; i++) {
        result = "" + value;
      }
      return result;
    }
  }

  public static void main(String[] args) {
    Runner.main(DoubleToStringBenchmarkSuite.class, args);
  }
}
