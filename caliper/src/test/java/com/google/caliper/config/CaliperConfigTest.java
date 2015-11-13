/*
 * Copyright (C) 2012 Google Inc.
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

package com.google.caliper.config;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import com.google.caliper.api.ResultProcessor;
import com.google.caliper.model.Trial;
import com.google.caliper.platform.Platform;
import com.google.caliper.platform.jvm.JvmPlatform;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.io.File;
import java.lang.management.ManagementFactory;

/**
 * Tests {@link CaliperConfig}.
 *
 * @author gak@google.com (Gregory Kick)
 */
@RunWith(JUnit4.class)
public class CaliperConfigTest {
  @Rule public TemporaryFolder folder = new TemporaryFolder();

  private Platform platform = new JvmPlatform();

  @Test public void getDefaultVmConfig() throws Exception {
    CaliperConfig configuration = new CaliperConfig(
        ImmutableMap.of("vm.args", "-very -special=args"));
    VmConfig defaultVmConfig = configuration.getDefaultVmConfig(platform);
    assertEquals(new File(System.getProperty("java.home")), defaultVmConfig.vmHome());
    ImmutableList<String> expectedArgs = new ImmutableList.Builder<String>()
        .addAll(ManagementFactory.getRuntimeMXBean().getInputArguments())
        .add("-very")
        .add("-special=args")
        .build();
    assertEquals(expectedArgs, defaultVmConfig.options());
  }

  @Test public void getVmConfig_baseDirectoryAndName() throws Exception {
    File tempBaseDir = folder.newFolder();
    File jdkHome = new File(tempBaseDir, "test");
    jdkHome.mkdir();
    CaliperConfig configuration = new CaliperConfig(ImmutableMap.of(
        "vm.baseDirectory", tempBaseDir.getAbsolutePath()));
    assertEquals(new VmConfig.Builder(platform, jdkHome).build(),
        configuration.getVmConfig(platform, "test"));
  }

  @Test public void getVmConfig_baseDirectoryAndHome() throws Exception {
    File tempBaseDir = folder.newFolder();
    File jdkHome = new File(tempBaseDir, "test-home");
    jdkHome.mkdir();
    CaliperConfig configuration = new CaliperConfig(ImmutableMap.of(
        "vm.baseDirectory", tempBaseDir.getAbsolutePath(),
        "vm.test.home", "test-home"));
    assertEquals(new VmConfig.Builder(platform, jdkHome).build(),
        configuration.getVmConfig(platform, "test"));
  }

  @Test public void getVmConfig() throws Exception {
    File jdkHome = folder.newFolder();
    CaliperConfig configuration = new CaliperConfig(ImmutableMap.of(
        "vm.args", "-a -b   -c",
        "vm.test.home", jdkHome.getAbsolutePath(),
        "vm.test.args", " -d     -e     "));
    assertEquals(
        new VmConfig.Builder(platform, jdkHome)
            .addOption("-a")
            .addOption("-b")
            .addOption("-c")
            .addOption("-d")
            .addOption("-e")
            .build(),
        configuration.getVmConfig(platform, "test"));
  }

  @Test public void getVmConfig_escapedSpacesInArgs() throws Exception {
    File jdkHome = folder.newFolder();
    CaliperConfig configuration = new CaliperConfig(ImmutableMap.of(
        "vm.args", "-a=string\\ with\\ spa\\ces -b -c",
        "vm.test.home", jdkHome.getAbsolutePath()));
    assertEquals(
        new VmConfig.Builder(platform, jdkHome)
        .addOption("-a=string with spaces")
        .addOption("-b")
        .addOption("-c")
        .build(),
        configuration.getVmConfig(platform, "test"));
  }

  @Test public void getInstrumentConfig() throws Exception {
    CaliperConfig configuration = new CaliperConfig(ImmutableMap.of(
        "instrument.test.class", "test.ClassName",
        "instrument.test.options.a", "1",
        "instrument.test.options.b", "excited b b excited"));
    assertEquals(
        new InstrumentConfig.Builder()
            .className("test.ClassName")
            .addOption("a", "1")
            .addOption("b", "excited b b excited")
            .build(),
        configuration.getInstrumentConfig("test"));
  }

  @Test public void getInstrumentConfig_notConfigured() throws Exception {
    CaliperConfig configuration = new CaliperConfig(ImmutableMap.of(
        "instrument.test.options.a", "1",
        "instrument.test.options.b", "excited b b excited"));
    try {
      configuration.getInstrumentConfig("test");
      fail();
    } catch (IllegalArgumentException expected) {}
  }

  @Test public void getConfiguredInstruments() throws Exception {
    CaliperConfig configuration = new CaliperConfig(ImmutableMap.of(
        "instrument.test.class", "test.ClassName",
        "instrument.test2.class", "test.ClassName",
        "instrument.test3.options.a", "1",
        "instrument.test4.class", "test.ClassName",
        "instrument.test4.options.b", "excited b b excited"));
    assertEquals(ImmutableSet.of("test", "test2", "test4"),
        configuration.getConfiguredInstruments());
  }

  @Test public void getConfiguredResultProcessors() throws Exception {
    assertEquals(ImmutableSet.of(),
        new CaliperConfig(ImmutableMap.<String, String>of()).getConfiguredResultProcessors());
    CaliperConfig configuration = new CaliperConfig(ImmutableMap.of(
        "results.test.class", TestResultProcessor.class.getName()));
    assertEquals(ImmutableSet.of(TestResultProcessor.class),
        configuration.getConfiguredResultProcessors());
  }

  @Test public void getResultProcessorConfig() throws Exception {
    CaliperConfig configuration = new CaliperConfig(ImmutableMap.of(
        "results.test.class", TestResultProcessor.class.getName(),
        "results.test.options.g", "ak",
        "results.test.options.c", "aliper"));
    assertEquals(
        new ResultProcessorConfig.Builder()
            .className(TestResultProcessor.class.getName())
            .addOption("g", "ak")
            .addOption("c", "aliper")
            .build(),
        configuration.getResultProcessorConfig(TestResultProcessor.class));
  }

  private static final class TestResultProcessor implements ResultProcessor {
    @Override public void close() {}

    @Override public void processTrial(Trial trial) {}
  }
}
