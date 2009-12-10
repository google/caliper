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

import java.util.*;

/**
 * Measures iterating through list elements.
 */
public class ListIterationBenchmarkSuite extends DefaultBenchmarkSuite {

  @Param private int length;

  private static Collection<Integer> lengthValues = Arrays.asList(0, 10, 100, 1000);

  private List<Object> list;
  private Object[] array;

  @Override protected void setUp() throws Exception {
    array = new Object[length];
    for (int i = 0; i < length; i++) {
      array[i] = new Object();
    }

    list = new AbstractList<Object>() {
      @Override public int size() {
        return length;
      }

      @Override public Object get(int i) {
        return array[i];
      }
    };
  }

  class ListIterateBenchmark extends Benchmark {
    @Override public Object run(int trials) throws Exception {
      int count = 0;
      for (int i = 0; i < trials; i++) {
        for (Object value : list) {
          count ^= (value == Boolean.TRUE) ? i : 0;
        }
      }
      return count > 0;
    }
  }

  class ArrayIterateBenchmark extends Benchmark {
    @Override public Object run(int trials) throws Exception {
      int count = 0;
      for (int i = 0; i < trials; i++) {
        for (Object value : array) {
          count ^= (value == Boolean.TRUE) ? i : 0;
        }
      }
      return count > 0;
    }
  }

  public static void main(String[] args) {
    Runner.main(ListIterationBenchmarkSuite.class, args);
  }
}