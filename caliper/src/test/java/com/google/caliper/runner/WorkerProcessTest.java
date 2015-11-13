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

package com.google.caliper.runner;

import static java.util.concurrent.TimeUnit.MINUTES;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import com.google.caliper.Benchmark;
import com.google.caliper.config.VmConfig;
import com.google.caliper.model.BenchmarkSpec;
import com.google.caliper.platform.jvm.JvmPlatform;
import com.google.caliper.worker.WorkerMain;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import com.google.common.collect.Sets;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.io.File;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Tests {@link WorkerProcess}.
 *
 * <p>TODO(lukes,gak): write more tests for how our specs get turned into commandlines
 */

@RunWith(JUnit4.class)
public class WorkerProcessTest {
  private static final int PORT_NUMBER = 4004;
  private static final UUID TRIAL_ID = UUID.randomUUID();

  private static class MockRegistrar implements ShutdownHookRegistrar {
    Set<Thread> hooks = Sets.newHashSet();
    @Override public void addShutdownHook(Thread hook) {
      hooks.add(hook);
    }
    @Override public boolean removeShutdownHook(Thread hook) {
      return hooks.remove(hook);
    }
  }

  private final MockRegistrar registrar = new MockRegistrar();
  private BenchmarkClass benchmarkClass;

  @Before public void setUp() throws InvalidBenchmarkException {
    benchmarkClass = BenchmarkClass.forClass(TestBenchmark.class);
  }

  @Test public void simpleArgsTest() throws Exception {
    Method method = TestBenchmark.class.getDeclaredMethods()[0];
    AllocationInstrument allocationInstrument = new AllocationInstrument();
    allocationInstrument.setOptions(ImmutableMap.of("trackAllocations", "true"));
    VmConfig vmConfig = new VmConfig(
        new File("foo"),
        Arrays.asList("--doTheHustle"),
        new File("java"),
        new JvmPlatform());
    Experiment experiment = new Experiment(
        allocationInstrument.createInstrumentation(method),
        ImmutableMap.<String, String>of(),
        new VirtualMachine("foo-jvm", vmConfig));
    BenchmarkSpec spec = new BenchmarkSpec.Builder()
        .className(TestBenchmark.class.getName())
        .methodName(method.getName())
        .build();
    ProcessBuilder builder = createProcess(experiment, spec);
    List<String> commandLine = builder.command();
    assertEquals(new File("java").getAbsolutePath(), commandLine.get(0));
    assertEquals("--doTheHustle", commandLine.get(1));  // vm specific flags come next
    assertEquals("-cp", commandLine.get(2));  // then the classpath
    // should we assert on classpath contents?
    ImmutableSet<String> extraCommandLineArgs =
        allocationInstrument.getExtraCommandLineArgs(vmConfig);
    assertEquals(extraCommandLineArgs.asList(),
        commandLine.subList(4, 4 + extraCommandLineArgs.size()));
    int index = 4 + extraCommandLineArgs.size();
    assertEquals("-XX:+PrintFlagsFinal", commandLine.get(index));
    assertEquals("-XX:+PrintCompilation", commandLine.get(++index));
    assertEquals("-XX:+PrintGC", commandLine.get(++index));
    assertEquals(WorkerMain.class.getName(), commandLine.get(++index));
    // followed by worker args...
  }

  @Test public void shutdownHook_waitFor() throws Exception {
    Process worker = createWorkerProcess(FakeWorkers.Exit.class, "0").startWorker();
    assertEquals("worker-shutdown-hook-" + TRIAL_ID,
        Iterables.getOnlyElement(registrar.hooks).getName());
    worker.waitFor();
    assertTrue(registrar.hooks.isEmpty());
  }

  @Test public void shutdownHook_exitValueThrows() throws Exception {
    Process worker = createWorkerProcess(
        FakeWorkers.Sleeper.class, Long.toString(MINUTES.toMillis(1))).startWorker();
    try {
      Thread hook = Iterables.getOnlyElement(registrar.hooks);
      assertEquals("worker-shutdown-hook-" + TRIAL_ID, hook.getName());
      try {
        worker.exitValue();
        fail();
      } catch (IllegalThreadStateException expected) {}
      assertTrue(registrar.hooks.contains(hook));
    } finally {
      worker.destroy(); // clean up
    }
  }

  @Test public void shutdownHook_exitValue() throws Exception {
    Process worker = createWorkerProcess(FakeWorkers.Exit.class, "0").startWorker();
    while (true) {
      try {
        worker.exitValue();
        assertTrue(registrar.hooks.isEmpty());
        break;
      } catch (IllegalThreadStateException e) {
        Thread.sleep(10);  // keep polling
      }
    }
  }

  @Test public void shutdownHook_destroy() throws Exception {
    Process worker = createWorkerProcess(
        FakeWorkers.Sleeper.class, Long.toString(MINUTES.toMillis(1))).startWorker();
    worker.destroy();
    assertTrue(registrar.hooks.isEmpty());
  }

  static final class TestBenchmark {
    @Benchmark long thing(long reps) {
      long dummy = 0;
      for (long i = 0; i < reps; i++) {
        dummy += new Long(dummy).hashCode();
      }
      return dummy;
    }
  }

  private ProcessBuilder createProcess(Experiment experiment, BenchmarkSpec benchmarkSpec) {
    return WorkerProcess.buildProcess(TRIAL_ID, experiment, benchmarkSpec, PORT_NUMBER,
        benchmarkClass);
  }

  private WorkerProcess createWorkerProcess(Class<?> main, String ...args) {
    return new WorkerProcess(FakeWorkers.createProcessBuilder(main, args),
        TRIAL_ID,
        null,
        registrar);
  }
}
