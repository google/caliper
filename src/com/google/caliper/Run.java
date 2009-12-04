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

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * An execution of a benchmark.
 */
public final class Run {

  private final Benchmark benchmark;
  private final BenchmarkSuite benchmarkSuite;
  private final Map<String, String> parameters;

  public Run(BenchmarkSuite benchmarkSuite, Benchmark benchmark, Map<String, String> parameters) {
    this.benchmark = benchmark;
    this.benchmarkSuite = benchmarkSuite;
    this.parameters = new LinkedHashMap<String, String>(parameters);
  }

  public Benchmark getBenchmark() {
    return benchmark;
  }

  public BenchmarkSuite getBenchmarkSuite() {
    return benchmarkSuite;
  }

  @Override public String toString() {
    return benchmark + " " + parameters;
  }
}
