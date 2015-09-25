/*
 * Copyright (C) 2010 Google Inc.
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

import com.google.caliper.BeforeExperiment;
import com.google.caliper.Benchmark;
import com.google.caliper.Param;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

public class ContainsBenchmark {
  @Param({"0", "25", "50", "75", "100"}) private int percentNulls;
  @Param({"100", "1000", "10000"}) private int containsPerRep;

  /** the set under test */
  private final Set<String> set = new HashSet<String>();

  /** twenty-five percent nulls */
  private final List<Object> queries = new ArrayList<Object>();

  @BeforeExperiment void setUp() {
    set.addAll(Arrays.asList("str1", "str2", "str3", "str4"));
    int nullThreshold = percentNulls * containsPerRep / 100;
    for (int i = 0; i < nullThreshold; i++) {
      queries.add(null);
    }
    for (int i = nullThreshold; i < containsPerRep; i++) {
      queries.add(new Object());
    }
    Collections.shuffle(queries, new Random(0));
  }

  @Benchmark void contains(int reps) {
    for (int i = 0; i < reps; i++) {
      for (Object query : queries) {
        set.contains(query);
      }
    }
  }
}
