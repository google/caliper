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
    testIt(0, "0ns");
    testIt(0, "0 ns");
    testIt(0, "0nanos");
    testIt(0, "0nanoseconds");
    testIt(0, "0ms");
    testIt(0, "0us");
    testIt(1000, "1 \u03bcs");
    testIt(500, "0.5us");
    testIt(500, "0.499000000000000000000000000000001us");
    testIt(60480000000000L, "0.7 days");
  }

  private void testIt(long i, String s) {
    assertEquals(i, SimpleDuration.valueOf(s).toNanos());
  }
}
