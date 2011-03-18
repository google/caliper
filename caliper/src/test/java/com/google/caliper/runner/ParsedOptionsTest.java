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
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableSetMultimap;
import com.google.common.collect.Iterables;
import com.google.common.io.Files;

import junit.framework.TestCase;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Method;

public class ParsedOptionsTest extends TestCase {
  private CaliperRc caliperRc;
  private File tempDir;

  @Override protected void setUp() throws IOException {
    tempDir = Files.createTempDir();
    makeTestVmTree(tempDir);

    ImmutableMap<String, String> map = ImmutableMap.of(
        "instrument.microbenchmark.defaultWarmupSeconds", "15",
        "vm.baseDirectory", tempDir.toString(),
        "instrument.alias.testInstrument", FakeInstrument.class.getName());
    caliperRc = new CaliperRc(map);
  }

  @Override protected void tearDown() throws IOException {
    if (tempDir != null) {
      Files.deleteRecursively(tempDir);
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
      ParsedOptions.from(new String[] {}, caliperRc);
      fail();
    } catch (InvalidCommandException expected) {
      assertEquals("No benchmark class specified", expected.getMessage());
    }
  }

  public void testHelp() throws InvalidCommandException {
    try {
      ParsedOptions.from(new String[] {"--help"}, caliperRc);
      fail();
    } catch (DisplayUsageException expected) {
    }
  }
  
  public void testDefaults() throws InvalidCommandException {
    CaliperOptions options = ParsedOptions.from(new String[] {CLASS_NAME}, caliperRc);

    assertEquals(CLASS_NAME, options.benchmarkClassName());
    assertTrue(options.benchmarkMethodNames().isEmpty());
    assertFalse(options.calculateAggregateScore());
    assertFalse(options.detailedLogging());
    assertFalse(options.dryRun());
    assertEquals(new MicrobenchmarkInstrument(), options.instrument());
    assertNull(options.outputFileOrDir());
    assertEquals(1, options.trials());
    assertTrue(options.userParameters().isEmpty());
    assertFalse(options.verbose());
    assertTrue(options.vmArguments().isEmpty());
    assertEquals(1, options.vms().size());
    assertEquals(15, options.warmupSeconds());
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
        "--warmup=20",
        "--delimiter=;",
        CLASS_NAME,
    };
    CaliperOptions options = ParsedOptions.from(args, caliperRc);

    assertEquals(CLASS_NAME, options.benchmarkClassName());
    assertEquals(ImmutableSet.of("foo", "bar", "qux"), options.benchmarkMethodNames());
    assertTrue(options.calculateAggregateScore());
    assertTrue(options.detailedLogging());
    assertFalse(options.dryRun());
    assertEquals(FakeInstrument.class, options.instrument().getClass());
    assertEquals("outputdir", options.outputFileOrDir());
    assertEquals(2, options.trials());
    assertEquals(ImmutableSetMultimap.of("x", "a", "x", "b", "x", "c", "y", "b", "y", "d"),
        options.userParameters());
    assertTrue(options.verbose());
    assertEquals(ImmutableSetMultimap.of("memoryMax", "-Xmx32m", "memoryMax", "-Xmx64m"),
        options.vmArguments());

    VirtualMachine vm = Iterables.getOnlyElement(options.vms());
    assertEquals("testVm", vm.name);
    assertEquals(new File(tempDir, "testVm/bin/java"), vm.execPath);

    assertEquals(20, options.warmupSeconds());
  }

  public static class FakeBenchmark extends Benchmark {}

  private static final String CLASS_NAME = FakeBenchmark.class.getName();

  public static class FakeInstrument extends Instrument {
    @Override public boolean isBenchmarkMethod(Method m) {
      return false;
    }

    @Override public BenchmarkMethod createBenchmarkMethod(BenchmarkClass c, Method m) {
      return null;
    }

    @Override public void dryRun(Benchmark b, BenchmarkMethod m) {}
  }
}
