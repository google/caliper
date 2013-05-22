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

package examples;

import com.google.caliper.Benchmark;
import com.google.caliper.Param;

import java.util.AbstractList;
import java.util.List;

/**
 * Measures iterating through list elements.
 */
public class ListIterationBenchmark extends Benchmark {

  @Param({"0", "10", "100", "1000"})
  private int length;

  private List<Object> list;
  private Object[] array;

  @Override protected void setUp() {
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

  @SuppressWarnings({"UnusedDeclaration"}) // TODO: fix
  public void timeListIteration(int reps) {
    for (int i = 0; i < reps; i++) {
      for (Object value : list) {
      }
    }
  }

  @SuppressWarnings({"UnusedDeclaration"}) // TODO: fix
  public void timeArrayIteration(int reps) {
    for (int i = 0; i < reps; i++) {
      for (Object value : array) {
      }
    }
  }
}