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

import java.util.Map;
import java.util.Set;

/**
 * A collection of benchmarks that share a set of configuration parameters.
 */
public interface Benchmark {

  Set<String> parameterNames();

  Set<String> parameterValues(String parameterName);

  ConfiguredBenchmark createBenchmark(Map<String, String> parameterValues);

  /**
   * A mapping of units to their values. Their values must be integers, but all values are relative,
   * so if one unit is 1.5 times the size of another, then these units can be expressed as
   * {"unit1"=10,"unit2"=15}. The smallest unit given by the function will be used to display
   * immediate results when running at the command line.
   *
   * e.g. 0% Scenario{...} 16.08<SMALLEST-UNIT>; σ=1.72<SMALLEST-UNIT> @ 3 trials
   */
  Map<String, Integer> timeUnitNames();

  Map<String, Integer> instanceUnitNames();

  Map<String, Integer> memoryUnitNames();

  /**
   * Converts nanoseconds to the smallest unit defined in {@link #timeUnitNames()}.
   */
  double nanosToUnits(double nanos);

  double instancesToUnits(long instances);

  double bytesToUnits(long bytes);
}