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

package com.google.caliper;

import com.google.common.base.Supplier;
import com.google.common.io.NullOutputStream;
import java.io.PrintStream;
import java.util.Map;
import java.util.Set;
import junit.framework.TestCase;

public final class CaliperTest extends TestCase {

  /**
   * Test we detect and fail when benchmarks don't scale properly.
   * @throws Exception
   */
  public void testBenchmarkScalesNonLinearly() throws Exception {
    TimeMeasurer timeMeasurer = new TimeMeasurer(1000, 1000, new PrintStream(new NullOutputStream()));
    try {
      timeMeasurer.run(new NonLinearTimedRunnable());
      fail();
    } catch (UserException.DoesNotScaleLinearlyException e) {
    }
  }

  private static class NonLinearTimedRunnable extends ConfiguredBenchmark
      implements Supplier<ConfiguredBenchmark> {
    private NonLinearTimedRunnable() {
      super(new NoOpBenchmark());
    }

    @Override public ConfiguredBenchmark get() {
      return this;
    }

    @Override public Object run(int reps) throws Exception {
      return null; // broken! doesn't loop reps times.
    }

    @Override public void close() throws Exception {}
  }

  private static class NoOpBenchmark implements Benchmark {
    @Override public Set<String> parameterNames() {
      return null;
    }

    @Override public Set<String> parameterValues(String parameterName) {
      return null;
    }

    @Override public ConfiguredBenchmark createBenchmark(Map<String, String> parameterValues) {
      return null;
    }

    @Override public Map<String, Integer> timeUnitNames() {
      return null;
    }

    @Override public Map<String, Integer> instanceUnitNames() {
      return null;
    }

    @Override public Map<String, Integer> memoryUnitNames() {
      return null;
    }

    @Override public double nanosToUnits(double nanos) {
      return 0;
    }

    @Override public double instancesToUnits(long instances) {
      return 0;
    }

    @Override public double bytesToUnits(long bytes) {
      return 0;
    }
  }
}
