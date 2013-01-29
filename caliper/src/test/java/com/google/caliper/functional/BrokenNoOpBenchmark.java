/**
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

package com.google.caliper.functional;

import com.google.caliper.Benchmark;
import com.google.caliper.Runner;

/**
 * This fails with a runtime out of range error.
 */
public class BrokenNoOpBenchmark extends Benchmark {

  public void timeNoOp(int reps) {
    for (int i = 0; i < reps; i++) {}
  }

  public static void main(String[] args) throws Exception {
    Runner.main(BrokenNoOpBenchmark.class, args);
  }
}
