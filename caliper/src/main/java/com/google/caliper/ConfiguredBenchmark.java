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

package com.google.caliper;

import java.util.Map;

public abstract class ConfiguredBenchmark {

  private final SimpleBenchmark underlyingBenchmark;

  protected ConfiguredBenchmark(SimpleBenchmark underlyingBenchmark) {
    this.underlyingBenchmark = underlyingBenchmark;
  }

  /**
   * Runs the benchmark through {@code reps} iterations.
   *
   * @return any object or null. Benchmark implementors may keep an accumulating
   *      value to prevent the runtime from optimizing away the code under test.
   *      Such an accumulator value can be returned here.
   */
  public abstract Object run(int reps) throws Exception;

  public abstract void close() throws Exception;

  public final SimpleBenchmark getBenchmark() {
    return underlyingBenchmark;
  }

  public final double nanosToUnits(double nanos) {
    return underlyingBenchmark.nanosToUnits(nanos);
  }

  public final Map<String, Integer> timeUnitNames() {
    return underlyingBenchmark.getTimeUnitNames();
  }

  public final double instancesToUnits(long instances) {
    return underlyingBenchmark.instancesToUnits(instances);
  }

  public final Map<String, Integer> instanceUnitNames() {
    return underlyingBenchmark.getInstanceUnitNames();
  }

  public final double bytesToUnits(long bytes) {
    return underlyingBenchmark.bytesToUnits(bytes);
  }

  public final Map<String, Integer> memoryUnitNames() {
    return underlyingBenchmark.getMemoryUnitNames();
  }
}
