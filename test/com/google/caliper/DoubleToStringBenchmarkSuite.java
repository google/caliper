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

import java.util.Arrays;

public class DoubleToStringBenchmarkSuite extends DefaultBenchmarkSuite {

  @Param private Double d;

  private static Iterable<Double> dValues = Arrays.asList(
      0.0d,
      Double.NEGATIVE_INFINITY,
      Double.NaN,
      Math.PI
  );

  class FormatterBenchmark extends Benchmark {
    public void run(int trials) {
      Double value = d;
      for (int i = 0; i < trials; i++) {
        String.format("%f", value);
      }
    }
  }

  class ToStringBenchmark extends Benchmark {
    public void run(int trials) {
      Double value = d;
      for (int i = 0; i < trials; i++) {
        value.toString();
      }
    }
  }

  class ConcatenationBenchmark extends Benchmark {
    public void run(int trials) {
      Double value = d;
      for (int i = 0; i < trials; i++) {
        String s = "" + value;
      }
    }
  }

  public static void main(String[] args) {
    Runner.main(DoubleToStringBenchmarkSuite.class.getName());
  }
}
