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

package test;

import com.google.caliper.Benchmark;
import com.google.caliper.TimedRunnable;
import com.google.caliper.Runner;
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

  public Set<String> parameterNames() {
    return delegate.parameterNames();
  }

  public Set<String> parameterValues(String parameterName) {
    return delegate.parameterValues(parameterName);
  }

  public TimedRunnable createBenchmark(Map<String, String> parameterValues) {
    final TimedRunnable benchmark = delegate.createBenchmark(parameterValues);

    return new TimedRunnable() {
      public Object run(int reps) throws Exception {
        // TODO: can we move the setup/tear down work out of the timed loop?
        Runtime.getRuntime().traceMethodCalls(true);
        try {
          return benchmark.run(reps);
        } finally {
          Runtime.getRuntime().traceMethodCalls(false);
        }
      }
    };
  }

  public static void main(String[] args) {
    Runner.main(TracingBenchmark.class);
  }
}
