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

package com.google.caliper.functional;

import com.google.caliper.SimpleBenchmark;
import com.google.caliper.Runner;

/**
 * Demonstrates that the benchmark can emit output without consequence.
 */
public class SystemOutAndErrBenchmark extends SimpleBenchmark {
  
  public void timeSystemOutAndSystemErr(int reps) {
    for (int i = 0; i < reps; i++) {
      System.out.println("hello, out");
      System.err.println("hello, err");
    }
  }

  public static void main(String[] args) {
    Runner.main(SystemOutAndErrBenchmark.class, args);
  }
}
