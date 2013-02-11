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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.util.concurrent.TimeUnit;

@RunWith(JUnit4.class)
public class ShortDurationTest {
  @Test public void valueOf() {
    assertEquals(ShortDuration.zero(), ShortDuration.valueOf("0"));
    testIt(0, "0ns", "0s");
    testIt(0, "0 ns", "0s");
    testIt(0, "0nanos", "0s");
    testIt(0, "0nanoseconds", "0s");
    testIt(0, "0 ms", "0s");
    testIt(0, "0us", "0s");
    testIt(0, "1e-12 ms", "0s");
    testIt(1, "0.501 ns", "0.501ns");
    testIt(1000, "1 \u03bcs", "1\u03bcs");
    // testIt(500, "0.5us", "500ns");
    // testIt(499, "0.499000000000000000000000000000001us", "499ns");
    // testIt(500, "0.49995 us", "500ns");
    // testIt(60480000000000L, "0.7 days", "16.80h");
    // testIt(Long.MAX_VALUE, "106751.99116730064591 days", "106751.99d");
  }

  @Test public void tooLongForALong() {
    try {
      ShortDuration.valueOf("106751.99116730064592 days");
      fail();
    } catch (ArithmeticException expected) {
    }
  }

  private static void testIt(long i, String s, String p) {
    ShortDuration d = ShortDuration.valueOf(s);
    assertEquals(i, d.to(TimeUnit.NANOSECONDS));
    assertEquals(p, d.toString());
  }
}
