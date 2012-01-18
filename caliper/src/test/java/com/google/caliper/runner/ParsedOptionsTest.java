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

import com.google.caliper.api.Benchmark;
import com.google.caliper.util.DisplayUsageException;
import com.google.caliper.util.InvalidCommandException;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableSetMultimap;
import com.google.common.collect.Iterables;
import com.google.common.io.Files;

import junit.framework.TestCase;

import java.io.File;
import java.io.IOException;

public class ParsedOptionsTest extends TestCase {
  private File tempDir;

  @Override protected void setUp() throws IOException {
    tempDir = Files.createTempDir();
    makeTestVmTree(tempDir);
  }

  @Override protected void tearDown() throws IOException {
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

  public void testNoOptions() throws InvalidCommandException {
    try {
      ParsedOptions.from(new String[] {});
      fail();
    } catch (InvalidCommandException expected) {
      assertEquals("No benchmark class specified", expected.getMessage());
    }
  }

  public void testHelp() throws InvalidCommandException {
    try {
      ParsedOptions.from(new String[] {"--help"});
      fail();
    } catch (DisplayUsageException expected) {
    }
  }
  
  public void testDefaults() throws InvalidCommandException {
    CaliperOptions options = ParsedOptions.from(new String[] {CLASS_NAME});

    assertEquals(CLASS_NAME, options.benchmarkClassName());
    assertTrue(options.benchmarkMethodNames().isEmpty());
    assertFalse(options.calculateAggregateScore());
    assertFalse(options.detailedLogging());
    assertFalse(options.dryRun());
    assertEquals("micro", options.instrumentName());
    assertNull(options.outputFileOrDir());
    assertEquals(1, options.trialsPerScenario());
    assertTrue(options.userParameters().isEmpty());
    assertFalse(options.verbose());
    assertTrue(options.vmArguments().isEmpty());
    assertEquals(0, options.vmNames().size());
  }

  public void testKitchenSink() throws InvalidCommandException {
    String[] args = {
        "--benchmark=foo;bar;qux",
        "--score",
        "--logging",
        "--instrument=testInstrument",
        "--output=outputdir",
        "--trials=2",
        "-Dx=a;b;c",
        "-Dy=b;d",
        "--verbose",
        "-JmemoryMax=-Xmx32m;-Xmx64m",
        "--vm=testVm",
        "--delimiter=;",
        CLASS_NAME,
    };
    CaliperOptions options = ParsedOptions.from(args);

    assertEquals(CLASS_NAME, options.benchmarkClassName());
    assertEquals(ImmutableSet.of("foo", "bar", "qux"), options.benchmarkMethodNames());
    assertTrue(options.calculateAggregateScore());
    assertTrue(options.detailedLogging());
    assertFalse(options.dryRun());
    assertEquals("testInstrument", options.instrumentName());
    assertEquals("outputdir", options.outputFileOrDir());
    assertEquals(2, options.trialsPerScenario());
    assertEquals(ImmutableSetMultimap.of("x", "a", "x", "b", "x", "c", "y", "b", "y", "d"),
        options.userParameters());
    assertTrue(options.verbose());
    assertEquals(ImmutableSetMultimap.of("memoryMax", "-Xmx32m", "memoryMax", "-Xmx64m"),
        options.vmArguments());

    String vmName = Iterables.getOnlyElement(options.vmNames());
    assertEquals("testVm", vmName);
  }

  public static class FakeBenchmark extends Benchmark {}

  private static final String CLASS_NAME = FakeBenchmark.class.getName();
}
