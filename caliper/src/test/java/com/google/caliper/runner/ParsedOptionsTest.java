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
import com.google.caliper.spi.Instrument;
import com.google.caliper.util.HelpRequestedException;
import com.google.caliper.util.InvalidCommandException;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableSetMultimap;
import com.google.common.collect.Iterables;
import com.google.common.io.InputSupplier;

import junit.framework.TestCase;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Method;

public class ParsedOptionsTest extends TestCase {
  private CaliperRc caliperRc;
  private FilesystemFacade filesystem;

  @Override protected void setUp() {
    ImmutableMap<String, String> map = ImmutableMap.of(
        "instrument.microbenchmark.defaultWarmupSeconds", "10",
        "vm.baseDirectory", "/vms/go/here",
        "vm.alias.testVm", "testvmhome/bin/java",
        "instrument.alias.testInstrument", FakeInstrument.class.getName());
    caliperRc = new CaliperRc(map);

    filesystem = new FakeFilesystem();
  }

  public void testNoOptions() throws InvalidCommandException {
    try {
      ParsedOptions.from(new String[] {}, filesystem, caliperRc);
      fail();
    } catch (InvalidCommandException expected) {
      assertEquals("No benchmark class specified", expected.getMessage());
    }
  }

  public void testHelp() throws InvalidCommandException {
    try {
      ParsedOptions.from(new String[] {"--help"}, filesystem, caliperRc);
      fail();
    } catch (HelpRequestedException expected) {
    }
  }
  public void testDefaults() throws InvalidCommandException {
    CaliperOptions options = ParsedOptions.from(new String[] {CLASS_NAME}, filesystem, caliperRc);

    assertEquals(new BenchmarkClass(FakeBenchmark.class), options.benchmarkClass());
    assertTrue(options.benchmarkNames().isEmpty());
    assertTrue(options.userParameters().isEmpty());
    assertFalse(options.calculateAggregateScore());
    assertFalse(options.dryRun());
    assertEquals(new MicrobenchmarkInstrument(), options.instrument());
    assertEquals(1, options.vms().size());
    assertTrue(options.vmArguments().isEmpty());
    assertNull(options.outputFileOrDir());
    assertEquals(1, options.trials());
    assertFalse(options.verbose());
    assertFalse(options.detailedLogging());
    assertEquals(10, options.warmupSeconds());
  }

  public void testEverything() throws InvalidCommandException {
    String[] args = {
        "--benchmark=foo;bar;qux",
        "--vm=testVm",
        "--instrument=testInstrument",
        "--trials=2",
        "--warmup=20",
        "--output=outputdir",
        "--logging",
        "--verbose",
        "--delimiter=;",
        "--score",
        "-Dx=a;b;c",
        "-Dy=b;d",
        "-JmemoryMax=-Xmx32m;-Xmx64m",
        CLASS_NAME,
    };
    CaliperOptions options = ParsedOptions.from(args, filesystem, caliperRc);

    assertEquals(new BenchmarkClass(FakeBenchmark.class), options.benchmarkClass());
    assertEquals(ImmutableSet.of("foo", "bar", "qux"), options.benchmarkNames());
    assertEquals(ImmutableSetMultimap.of("x", "a", "x", "b", "x", "c", "y", "b", "y", "d"),
        options.userParameters());
    assertTrue(options.calculateAggregateScore());
    assertFalse(options.dryRun());
    assertEquals(FakeInstrument.class, options.instrument().getClass());

    VirtualMachine vm = Iterables.getOnlyElement(options.vms());
    assertEquals("testVm", vm.name);
    assertEquals("/vms/go/here/testvmhome/bin/java", vm.execPath);

    assertEquals(ImmutableSetMultimap.of("memoryMax", "-Xmx32m", "memoryMax", "-Xmx64m"),
        options.vmArguments());

    assertEquals("outputdir", options.outputFileOrDir());
    assertEquals(2, options.trials());
    assertTrue(options.verbose());
    assertTrue(options.detailedLogging());
    assertEquals(20, options.warmupSeconds());
  }

  public static class FakeBenchmark extends Benchmark {}

  private static final String CLASS_NAME = FakeBenchmark.class.getName();

  public static class FakeInstrument extends Instrument {
    @Override public boolean isBenchmarkMethod(Method method) {
      return false;
    }

    @Override
    public BenchmarkMethod createBenchmarkMethod(BenchmarkClass benchmarkClass, Method method) {
      return null;
    }

    @Override public void dryRun(Scenario scenario) {
    }
  }

  static class FakeFilesystem implements FilesystemFacade {
    @Override public boolean exists(String filename) {
      return filename.equals("/vms/go/here/testvmhome")
          || filename.equals("/vms/go/here/testvmhome/bin/java");
    }

    @Override public boolean isDirectory(String filename) {
      return filename.equals("/vms/go/here/testvmhome");
    }

    @Override
    public void copy(InputSupplier<InputStream> in, String rcFileName) throws IOException {
    }

    @Override
    public ImmutableMap<String, String> loadProperties(String propFileName) throws IOException {
      return null;
    }

    @Override public String makeAbsolute(String name, String baseDir) {
      return name.startsWith("/")
          ? name
          : baseDir + "/" + name;
    }
  }
}
