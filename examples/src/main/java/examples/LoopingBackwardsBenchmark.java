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

import com.google.caliper.Benchmark;
import com.google.caliper.Param;
import com.google.caliper.Runner;

/**
 * Testing the old canard that looping backwards is faster.
 */
public class LoopingBackwardsBenchmark extends Benchmark {
  @Param({"2", "20", "2000", "20000000"}) int max;

  public int timeForwards(int reps) {
    int dummy = 0;
    for (int i = 0; i < reps; i++) {
      for (int j = 0; j < max; j++) {
        dummy += j;
      }
    }
    return dummy;
  }

  public int timeBackwards(int reps) {
    int dummy = 0;
    for (int i = 0; i < reps; i++) {
      for (int j = max - 1; j >= 0; j--) {
        dummy += j;
      }
    }
    return dummy;
  }

  public static void main(String[] args) throws Exception {
    Runner.main(LoopingBackwardsBenchmark.class, args);
  }
}
