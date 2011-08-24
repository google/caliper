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

import com.google.caliper.Benchmark;
import com.google.caliper.ConfiguredBenchmark;
import com.google.caliper.Runner;
import com.google.common.collect.ImmutableMap;
import java.util.Set;
import java.util.Map;

/**
 * Proof-of-concept of a decorating benchmark.
 */
public class TracingBenchmark implements Benchmark {

  private final Benchmark delegate;

  public TracingBenchmark() {
    this.delegate = new ThreadSleepBenchmark();
  }

  @Override public Set<String> parameterNames() {
    return delegate.parameterNames();
  }

  @Override public Set<String> parameterValues(String parameterName) {
    return delegate.parameterValues(parameterName);
  }

  @Override public ConfiguredBenchmark createBenchmark(Map<String, String> parameterValues) {
    final ConfiguredBenchmark benchmark = delegate.createBenchmark(parameterValues);

    return new ConfiguredBenchmark(benchmark.getBenchmark()) {
      @Override public Object run(int reps) throws Exception {
        // TODO: can we move the setup/tear down work out of the timed loop?
        Runtime.getRuntime().traceMethodCalls(true);
        try {
          return benchmark.run(reps);
        } finally {
          Runtime.getRuntime().traceMethodCalls(false);
        }
      }

      @Override public void close() throws Exception {
        benchmark.close();
      }
    };
  }

  @Override public Map<String, Integer> getTimeUnitNames() {
    return ImmutableMap.of("ns", 1, "us", 1000, "ms", 1000000, "s", 1000000000);
  }

  @Override public double nanosToUnits(double nanos) {
    return nanos;
  }

  @Override public Map<String, Integer> getInstanceUnitNames() {
    return ImmutableMap.of(" instances", 1, "K instances", 1000, "M instances", 1000000,
        "B instances", 1000000000);
  }

  @Override public double instancesToUnits(long instances) {
    return instances;
  }

  @Override public Map<String, Integer> getMemoryUnitNames() {
    return ImmutableMap.of("B", 1, "KB", 1024, "MB", 1048576, "GB", 1073741824);
  }

  @Override public double bytesToUnits(long bytes) {
    return bytes;
  }

  public static void main(String[] args) {
    Runner.main(TracingBenchmark.class, args);
  }
}
