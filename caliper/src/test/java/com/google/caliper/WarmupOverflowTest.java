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

import com.google.caliper.UserException.DoesNotScaleLinearlyException;
import com.google.common.util.concurrent.SimpleTimeLimiter;
import com.google.common.util.concurrent.TimeLimiter;

import junit.framework.TestCase;

import java.util.concurrent.Callable;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * Test exposing an issue where any warmup that completes enough executions to reach
 * Integer.MAX_VALUE reps (either because the benchmark code is optimized away or because the
 * warmupMillis are long enough compared to the benchmark execution time).
 */
public class WarmupOverflowTest extends TestCase {
  private TimeLimiter timeLimiter;

  @Override public void setUp() {
    timeLimiter = new SimpleTimeLimiter(Executors.newSingleThreadExecutor());
  }


  public void testOptimizedAwayBenchmarkDoesNotTakeTooLongToRun() throws Exception {
    try {
      timeLimiter.callWithTimeout(new Callable<Void>() {
        @Override public Void call() throws Exception {
          InProcessRunner runner = new InProcessRunner();
          runner.run(OptimizedAwayBenchmark.class.getName(), "--warmupMillis", "3000",
              "--measurementType", "TIME");
          return null;
        }
      }, 90, TimeUnit.SECONDS, false);
    } catch (DoesNotScaleLinearlyException expected) {
    }
  }


  public void testLongWarmupMillisDoesNotTakeTooLongToRun() throws Exception {
    timeLimiter.callWithTimeout(new Callable<Void>() {
      @Override public Void call() throws Exception {
        InProcessRunner runner = new InProcessRunner();
        runner.run(RelativelyFastBenchmark.class.getName(), "--warmupMillis", "8000",
            "--runMillis", "51", "--measurementType", "TIME");
        return null;
      }
    }, 90, TimeUnit.SECONDS, false);
  }

  public static class OptimizedAwayBenchmark extends SimpleBenchmark {
    public void timeIsNullOrEmpty(int reps) {
      for (int i = 0; i < reps; i++) {
        // do nothing!
      }
    }
  }

  public static class RelativelyFastBenchmark extends SimpleBenchmark {
    public long timeSqrt(int reps) {
      long result = 0;
      for(int i = 0; i < reps; i++) {
        result += Math.sqrt(81);
      }
      return result;
    }
  }
}
