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

import com.google.common.collect.ImmutableMap;

import java.util.Map;

/**
 * A configured benchmark.
 */
final class Run {

  private final Class<? extends Benchmark> benchmarkClass;
  private final ImmutableMap<String, String> parameters;

  public Run(Class<? extends Benchmark> benchmarkClass,
      Map<String, String> parameters) {
    this.benchmarkClass = benchmarkClass;
    this.parameters = ImmutableMap.copyOf(parameters);
  }

  public Class<? extends Benchmark> getBenchmarkClass() {
    return benchmarkClass;
  }

  public ImmutableMap<String, String> getParameters() {
    return parameters;
  }

  @Override public String toString() {
    return benchmarkClass.getSimpleName() + " " + parameters;
  }
}
