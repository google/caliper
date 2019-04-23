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

package com.google.caliper.runner.target;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import com.google.caliper.Benchmark;
import com.google.caliper.core.BenchmarkClassModel;
import com.google.caliper.core.BenchmarkClassModel.MethodModel;
import com.google.caliper.runner.config.VmConfig;
import com.google.caliper.runner.config.VmType;
import com.google.caliper.runner.experiment.Experiment;
import com.google.caliper.runner.instrument.AllocationInstrument;
import com.google.caliper.runner.target.VmProcess.Logger;
import com.google.caliper.runner.testing.FakeWorkerSpec;
import com.google.caliper.runner.testing.FakeWorkers;
import com.google.caliper.runner.worker.WorkerSpec;
import com.google.caliper.runner.worker.trial.TrialSpec;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Iterables;
import com.google.common.collect.Sets;
import java.io.File;
import java.util.Set;
import java.util.UUID;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/**
 * Tests {@link LocalDevice}.
 *
 * <p>TODO(lukes,gak): write more tests for how our specs get turned into commandlines
 */

@RunWith(JUnit4.class)
public class LocalDeviceTest {
  private static final int PORT_NUMBER = 4004;
  private static final UUID TRIAL_ID = UUID.randomUUID();

  private static class MockRegistrar implements ShutdownHookRegistrar {
    Set<Thread> hooks = Sets.newHashSet();

    @Override
    public void addShutdownHook(Thread hook) {
      hooks.add(hook);
    }

    @Override
    public boolean removeShutdownHook(Thread hook) {
      return hooks.remove(hook);
    }
  }

  private final MockRegistrar registrar = new MockRegistrar();
  private final LocalDevice device = LocalDevice.builder().shutdownHookRegistrar(registrar).build();

  @Before
  public void startDevice() {
    device.startAsync().awaitRunning();
  }

  @After
  public void stopDevice() {
    device.stopAsync().awaitTerminated();
  }

  @Test
  public void simpleArgsTest() throws Exception {
    MethodModel method = MethodModel.of(TestBenchmark.class.getDeclaredMethods()[0]);
    AllocationInstrument allocationInstrument = new AllocationInstrument();
    allocationInstrument.setOptions(ImmutableMap.of("trackAllocations", "true"));
    VmConfig vmConfig =
        VmConfig.builder()
            .name("foo-jvm")
            .type(VmType.JVM)
            .home(System.getProperty("java.home"))
            .addArg("--doTheHustle")
            .build();
    Target target = device.createTarget(vmConfig);
    Experiment experiment =
        Experiment.create(
            1,
            allocationInstrument.createInstrumentedMethod(method),
            ImmutableMap.<String, String>of(),
            target);
    BenchmarkClassModel benchmarkClass = BenchmarkClassModel.create(TestBenchmark.class);
    ImmutableList<String> commandLine = createCommand(experiment, benchmarkClass);
    assertThat(commandLine.get(0)).startsWith(System.getProperty("java.home") + "/bin/java");
    assertThat(commandLine).contains("--doTheHustle");
    assertThat(commandLine).contains("-cp");
    assertThat(commandLine).containsAtLeastElementsIn(target.vm().trialArgs());
    assertThat(commandLine)
        .containsAtLeastElementsIn(allocationInstrument.getExtraCommandLineArgs(vmConfig));
    assertThat(commandLine)
        .containsAllOf("-XX:+PrintFlagsFinal", "-XX:+PrintCompilation", "-XX:+PrintGC");
    // main class should be fourth to last, followed worker options
    assertEquals("com.google.caliper.worker.WorkerMain", commandLine.get(commandLine.size() - 4));
  }

  @Test
  public void shutdownHook_awaitExit() throws Exception {
    WorkerSpec spec = FakeWorkerSpec.builder(FakeWorkers.Exit.class).setArgs("0").build();
    VmProcess worker = device.startVm(spec, new NullLogger());
    assertEquals(
        "worker-shutdown-hook-" + spec.id(), Iterables.getOnlyElement(registrar.hooks).getName());
    worker.awaitExit();
    assertTrue(registrar.hooks.isEmpty());
  }

  @Test
  public void vmExecutablePath() {
    VmConfig config =
        VmConfig.builder()
            .name("foo")
            .type(VmType.JVM)
            .home(System.getProperty("java.home"))
            .build();
    Vm vm = Jvm.create(config, "classpath");
    String path = device.vmExecutablePath(vm);
    File javaExecutable = new File(path);
    assertTrue("Could not find: " + javaExecutable, javaExecutable.exists());
    assertTrue(javaExecutable + " is not a file", javaExecutable.isFile());
  }

  static final class TestBenchmark {
    @Benchmark
    long thing(long reps) {
      long dummy = 0;
      for (long i = 0; i < reps; i++) {
        dummy += new Long(dummy).hashCode();
      }
      return dummy;
    }
  }

  private ImmutableList<String> createCommand(
      Experiment experiment, BenchmarkClassModel benchmarkClass) {
    WorkerSpec spec = new TrialSpec(TRIAL_ID, PORT_NUMBER, experiment, benchmarkClass, 1);
    return device.createCommand(spec);
  }

  private static class NullLogger implements Logger {
    @Override
    public void log(String line) {}

    @Override
    public void log(String source, String line) {}
  }
}
