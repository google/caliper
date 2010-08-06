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
import junit.framework.TestCase;

public final class CaliperTest extends TestCase {

  /**
   * Test we detect and fail when benchmarks don't scale properly.
   * @throws Exception
   */
  public void testBenchmarkScalesNonLinearly() throws Exception {
    Caliper caliper = new Caliper(1000, 1000, new PrintStream(new NullOutputStream()));
    try {
      caliper.warmUp(new NonLinearTimedRunnable());
      fail();
    } catch (UserException.DoesNotScaleLinearlyException e) {
    }
  }

  private static class NonLinearTimedRunnable implements Supplier<TimedRunnable>, TimedRunnable {
    public TimedRunnable get() {
      return this;
    }
    public Object run(int reps) throws Exception {
      return null; // broken! doesn't loop reps times.
    }
    public void close() throws Exception {}
  }
}
