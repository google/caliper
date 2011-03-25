/*
 * Copyright (C) 2011 Google Inc.
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

package com.google.caliper.util;

import junit.framework.TestCase;

/**
 * @author Kevin Bourrillion
 */
public class SimpleDurationTest extends TestCase {
  public void test() {
    testIt(0, "0ns", "0.00ns");
    testIt(0, "0 ns", "0.00ns");
    testIt(0, "0nanos", "0.00ns");
    testIt(0, "0nanoseconds", "0.00ns");
    testIt(0, "0 ms", "0.00ns");
    testIt(0, "0us", "0.00ns");
    testIt(1, "1e-12 ms", "1.00ns"); // rounds up
    testIt(1000, "1 \u03bcs", "1.00\u03bcs");
    testIt(500, "0.5us", "500.00ns");
    testIt(500, "0.499000000000000000000000000000001us", "500.00ns");
    testIt(60480000000000L, "0.7 days", "16.80h");
    testIt(Long.MAX_VALUE, "106751.99116730064591 days", "106751.99d");
  }

  public void tooLongForALong() {
    try {
      SimpleDuration.valueOf("106751.99116730064592 days");
      fail();
    } catch (ArithmeticException expected) {
    }
  }

  private void testIt(long i, String s, String p) {
    SimpleDuration d = SimpleDuration.valueOf(s);
    assertEquals(i, d.toNanos());
    assertEquals(p, d.toString());
  }
}
