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

package com.google.caliper.options;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import com.google.caliper.util.DisplayUsageException;
import com.google.caliper.util.InvalidCommandException;
import com.google.caliper.util.ShortDuration;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableSetMultimap;
import com.google.common.collect.Iterables;
import com.google.common.io.Files;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.io.File;
import java.io.IOException;

@RunWith(JUnit4.class)

public class ParsedOptionsTest {
  private File tempDir;

  @Before public void setUp() throws IOException {
    tempDir = Files.createTempDir();
    makeTestVmTree(tempDir);
  }

  @After public void tearDown() throws IOException {
    if (tempDir != null) {
      Runtime.getRuntime().exec(new String[] {"rm", "-rf", tempDir.getCanonicalPath()});
    }
  }

  private static void makeTestVmTree(File baseDir) throws IOException {
    File bin = new File(baseDir, "testVm/bin");
    bin.mkdirs();
    File java = new File(bin, "java");
    Files.touch(java);
  }

  @Test public void testNoOptions_RequireBenchmarkClassName() {
    try {
      ParsedOptions.from(new String[] {}, true);
      fail();
    } catch (InvalidCommandException expected) {
      assertEquals("No benchmark class specified", expected.getMessage());
    }
  }

  @Test public void testTooManyArguments_RequireBenchmarkClassName() {
    try {
      ParsedOptions.from(new String[] {"a", "b"}, true);
      fail();
    } catch (InvalidCommandException expected) {
      assertEquals("Extra stuff, expected only class name: [a, b]", expected.getMessage());
    }
  }

  @Test public void testTooManyArguments_DoNotRequireBenchmarkClassName() {
    try {
      ParsedOptions.from(new String[] {"a", "b"}, false);
      fail();
    } catch (InvalidCommandException expected) {
      assertEquals("Extra stuff, did not expect non-option arguments: [a, b]",
          expected.getMessage());
    }
  }

  @Test public void testHelp() throws InvalidCommandException {
    try {
      ParsedOptions.from(new String[] {"--help"}, true);
      fail();
    } catch (DisplayUsageException expected) {
    }
  }

  @Test public void testDefaults_RequireBenchmarkClassName() throws InvalidCommandException {
    CaliperOptions options = ParsedOptions.from(new String[] {CLASS_NAME}, true);

    assertEquals(CLASS_NAME, options.benchmarkClassName());
    checkDefaults(options);
  }

  @Test public void testDefaults_DoNotRequireBenchmarkClassName() throws InvalidCommandException {
    CaliperOptions options = ParsedOptions.from(new String[] {}, false);

    assertNull(options.benchmarkClassName());
    checkDefaults(options);
  }

  private void checkDefaults(CaliperOptions options) {
    assertTrue(options.benchmarkMethodNames().isEmpty());
    assertFalse(options.dryRun());
    ImmutableSet<String> expectedInstruments = new ImmutableSet.Builder<String>()
        .add("allocation")
        .add("runtime")
        .build();
    assertEquals(expectedInstruments, options.instrumentNames());
    assertEquals(1, options.trialsPerScenario());
    assertTrue(options.userParameters().isEmpty());
    assertFalse(options.printConfiguration());
    assertTrue(options.vmArguments().isEmpty());
    assertEquals(0, options.vmNames().size());
  }

  @Test public void testKitchenSink() throws InvalidCommandException {
    String[] args = {
        "--benchmark=foo;bar;qux",
        "--instrument=testInstrument",
        "--directory=/path/to/some/dir",
        "--trials=2",
        "--time-limit=15s",
        "-Dx=a;b;c",
        "-Dy=b;d",
        "-Csome.property=value",
        "-Csome.other.property=other-value",
        "--print-config",
        "-JmemoryMax=-Xmx32m;-Xmx64m",
        "--vm=testVm",
        "--delimiter=;",
        CLASS_NAME,
    };
    CaliperOptions options = ParsedOptions.from(args, true);

    assertEquals(CLASS_NAME, options.benchmarkClassName());
    assertEquals(ImmutableSet.of("foo", "bar", "qux"), options.benchmarkMethodNames());
    assertFalse(options.dryRun());
    assertEquals(ImmutableSet.of("testInstrument"), options.instrumentNames());
    assertEquals(new File("/path/to/some/dir"), options.caliperDirectory());
    assertEquals(2, options.trialsPerScenario());
    assertEquals(ShortDuration.of(15, SECONDS), options.timeLimit());
    assertEquals(ImmutableSetMultimap.of("x", "a", "x", "b", "x", "c", "y", "b", "y", "d"),
        options.userParameters());
    assertEquals(ImmutableMap.of("some.property", "value", "some.other.property", "other-value"),
        options.configProperties());
    assertTrue(options.printConfiguration());
    assertEquals(ImmutableSetMultimap.of("memoryMax", "-Xmx32m", "memoryMax", "-Xmx64m"),
        options.vmArguments());

    String vmName = Iterables.getOnlyElement(options.vmNames());
    assertEquals("testVm", vmName);
  }

  public static class FakeBenchmark {}

  private static final String CLASS_NAME = FakeBenchmark.class.getName();
}
