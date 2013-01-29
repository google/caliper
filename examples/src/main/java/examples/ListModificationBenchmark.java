/*
 * Copyright (C) 2012 Google Inc.
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
import com.google.caliper.runner.CaliperMain;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

/**
 * Measures performance of list operations.
 */
public class ListModificationBenchmark extends Benchmark {

  private enum Element {
    INSTANCE,
  }
  private enum ListImpl {
    Array {
      @Override List<Element> create() {
        return new ArrayList<Element>();
      }
    },
    Linked {
      @Override List<Element> create() {
        return new LinkedList<Element>();
      }
    };

    abstract List<Element> create();
  }

  @Param({"10", "100", "1000", "10000"})
  private int size;

  @Param({"Array", "Linked"})
  private ListImpl implementation;

  private List<Element> list;

  @Override protected void setUp() throws Exception {
    list = implementation.create();
    for (int i = 0; i < size; i++) {
      list.add(Element.INSTANCE);
    }
  }

  public void timePopulate(int reps) throws Exception {
    for (int rep = 0; rep < reps; rep++) {
      List<Element> list = implementation.create();
      for (int i = 0; i < size; i++) {
        list.add(Element.INSTANCE);
      }
    }
  }

  public void timeIteration(int reps) {
    for (int rep = 0; rep < reps; rep++) {
      Iterator<Element> iterator = list.iterator();
      while (iterator.hasNext()) {
        iterator.next();
      }
    }
  }

  public void timeHeadAddRemove(int reps) {
    for (int rep = 0; rep < reps; rep++) {
      list.add(0, Element.INSTANCE);
      list.remove(0);
    }
  }

  public void timeMiddleAddRemove(int reps) {
    int index = size / 2;
    for (int rep = 0; rep < reps; rep++) {
      list.add(index, Element.INSTANCE);
      list.remove(index);
    }
  }

  public void timeTailAddRemove(int reps) {
    int index = size - 1;
    for (int rep = 0; rep < reps; rep++) {
      list.add(Element.INSTANCE);
      list.remove(index);
    }
  }

  // TODO: remove this from all examples when IDE plugins are ready
  public static void main(String[] args) throws Exception {
    CaliperMain.main(ListModificationBenchmark.class, args);
  }
}
