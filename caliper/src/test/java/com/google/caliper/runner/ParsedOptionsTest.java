/*
 * Copyright (C) 2011 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */

package com.google.caliper.runner;

import static java.util.Arrays.asList;

import com.google.caliper.util.InvalidCommandException;

import junit.framework.TestCase;

public class ParsedOptionsTest extends TestCase {

  // Just a placeholder for now

  public void testNoOptions() throws InvalidCommandException {
    // TODO(kevinb): obviously use a benchmark class in place of String
    CaliperOptions options = ParsedOptions.from(String.class);

    assertEquals(String.class, options.benchmarkClass());
    assertTrue(options.benchmarkMethodNames().isEmpty());
    assertTrue(options.benchmarkParameters().isEmpty());
    assertFalse(options.calculateAggregateScore());
    assertFalse(options.dryRun());
    assertEquals(asList("time"), options.instrumentNames());
    assertNull(options.jreBaseDir());
    assertNull(options.jreHomeDirs());
    assertTrue(options.jvmArguments().isEmpty());
    assertNull(options.outputFileOrDir());
    assertEquals(1, options.trials());
    assertFalse(options.verbose());
    assertEquals(10, options.warmupSeconds());
  }

  public void testNoClass() {
    try {
      ParsedOptions.from(null);
      fail();
    } catch (InvalidCommandException expected) {
      assertEquals("No benchmark class specified", expected.getMessage());
    }
  }
}
