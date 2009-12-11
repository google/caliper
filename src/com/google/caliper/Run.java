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

import java.lang.reflect.Method;
import java.util.Map;

/**
 * A configured benchmark.
 */
final class Run {

  private final ImmutableMap<String, String> parameters;
  private final Method benchmarkMethod;
  private final String vm;

  public Run(Map<String, String> parameters,
      Method benchmarkMethod,
      String vm) {
    this.benchmarkMethod = benchmarkMethod;
    this.parameters = ImmutableMap.copyOf(parameters);
    this.vm = vm;
  }

  public ImmutableMap<String, String> getParameters() {
    return parameters;
  }

  public Method getBenchmarkMethod() {
    return benchmarkMethod;
  }

  public String getVm() {
    return vm;
  }

  @Override public String toString() {
    return benchmarkMethod.getName() + " " + parameters;
  }
}
