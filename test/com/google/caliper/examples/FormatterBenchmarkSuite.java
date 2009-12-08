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

import java.util.Formatter;

/**
 * Compares Formatter against hand-written StringBuilder code.
 */
public class FormatterBenchmarkSuite extends DefaultBenchmarkSuite {

  class Formatter_NoFormatting extends Benchmark {
    public Object run(int trials) {
      for (int i = 0; i < trials; i++) {
        Formatter f = new Formatter();
        f.format("this is a reasonably short string that doesn't actually need any formatting");
      }
      return null;
    }
  }

  class StringBuilder_NoFormatting extends Benchmark {
    public Object run(int trials) {
      for (int i = 0; i < trials; i++) {
        StringBuilder sb = new StringBuilder();
        sb.append("this is a reasonably short string that doesn't actually need any formatting");
      }
      return null;
    }
  }

  class Formatter_OneInt extends Benchmark {
    public Object run(int trials) {
      for (int i = 0; i < trials; i++) {
        Formatter f = new Formatter();
        f.format("this is a reasonably short string that has an int %d in it", i);
      }
      return null;
    }
  }

  class StringBuilder_OneInt extends Benchmark {
    public Object run(int trials) {
      for (int i = 0; i < trials; i++) {
        StringBuilder sb = new StringBuilder();
        sb.append("this is a reasonably short string that has an int ");
        sb.append(i);
        sb.append(" in it");
      }
      return null;
    }
  }

  class Formatter_OneString extends Benchmark {
    public Object run(int trials) {
      for (int i = 0; i < trials; i++) {
        Formatter f = new Formatter();
        f.format("this is a reasonably short string that has a string %s in it", "hello");
      }
      return null;
    }
  }

  class StringBuilder_OneString extends Benchmark {
    public Object run(int trials) {
      for (int i = 0; i < trials; i++) {
        StringBuilder sb = new StringBuilder();
        sb.append("this is a reasonably short string that has a string ");
        sb.append("hello");
        sb.append(" in it");
      }
      return null;
    }
  }

  public static void main(String[] args) {
    Runner.main(FormatterBenchmarkSuite.class, args);
  }
}
