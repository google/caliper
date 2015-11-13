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

import com.google.caliper.bridge.CommandLineSerializer;
import com.google.caliper.bridge.OpenedSocket;
import com.google.caliper.bridge.WorkerSpec;
import com.google.caliper.config.VmConfig;
import com.google.caliper.model.BenchmarkSpec;
import com.google.caliper.runner.Instrument.Instrumentation;
import com.google.caliper.worker.WorkerMain;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.util.concurrent.ListenableFuture;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.logging.Logger;

import javax.annotation.concurrent.GuardedBy;
import javax.inject.Inject;

/**
 * A representation of an unstarted worker.
 *
 * <p>A worker is a sub process that runs a benchmark trial.  Specifically it is a JVM running
 * {@link com.google.caliper.worker.WorkerMain}.  Because of this we can make certain assumptions
 * about its behavior, including but not limited to:
 *
 * <ul>
 *   <li>The worker will connect back to us over a socket connection and send us UTF-8 json
 *       messages in a line oriented protocol.
 *   <li>TODO(lukes,gak): This is probably as good a place as any to specify the entire protocol.
 * </ul>
 */
@TrialScoped final class WorkerProcess {
  private static final Logger logger = Logger.getLogger(WorkerProcess.class.getName());

  @GuardedBy("this")
  private Process worker;
  private final ProcessBuilder workerBuilder;
  private final ShutdownHookRegistrar shutdownHookRegistrar;
  private final ListenableFuture<OpenedSocket> openedSocket;
  private final UUID trialId;

  @VisibleForTesting WorkerProcess(ProcessBuilder workerBuilder,
      UUID trialId,
      ListenableFuture<OpenedSocket> openedSocket,
      ShutdownHookRegistrar shutdownHookRegistrar) {
    this.trialId = trialId;
    this.workerBuilder = workerBuilder;
    this.openedSocket = openedSocket;
    this.shutdownHookRegistrar = shutdownHookRegistrar;
  }

  @Inject WorkerProcess(@TrialId UUID trialId,
      ListenableFuture<OpenedSocket> openedSocket,
      Experiment experiment,
      BenchmarkSpec benchmarkSpec,
      @LocalPort int localPort,
      BenchmarkClass benchmarkClass,
      ShutdownHookRegistrar shutdownHookRegistrar) {
    this.trialId = trialId;
    this.workerBuilder =
        buildProcess(trialId, experiment, benchmarkSpec, localPort, benchmarkClass);
    this.openedSocket = openedSocket;
    this.shutdownHookRegistrar = shutdownHookRegistrar;
  }

  ListenableFuture<OpenedSocket> socketFuture() {
    return openedSocket;
  }

  /**
   * Returns a {@link Process} representing this worker.  The process will be started if it hasn't
   * already.
   */
  synchronized Process startWorker() throws IOException {
    if (worker == null) {
      final Process delegate = workerBuilder.start();
      final Thread shutdownHook = new Thread("worker-shutdown-hook-" + trialId) {
        @Override public void run() {
          delegate.destroy();
        }
      };
      shutdownHookRegistrar.addShutdownHook(shutdownHook);
      worker = new Process() {
        @Override public OutputStream getOutputStream() {
          return delegate.getOutputStream();
        }

        @Override public InputStream getInputStream() {
          return delegate.getInputStream();
        }

        @Override public InputStream getErrorStream() {
          return delegate.getErrorStream();
        }

        @Override public int waitFor() throws InterruptedException {
          int waitFor = delegate.waitFor();
          shutdownHookRegistrar.removeShutdownHook(shutdownHook);
          return waitFor;
        }

        @Override public int exitValue() {
          int exitValue = delegate.exitValue();
          // if it hasn't thrown, the process is done
          shutdownHookRegistrar.removeShutdownHook(shutdownHook);
          return exitValue;
        }

        @Override public void destroy() {
          delegate.destroy();
          shutdownHookRegistrar.removeShutdownHook(shutdownHook);
        }
      };
    }
    return worker;
  }
  
  @VisibleForTesting static ProcessBuilder buildProcess(
      UUID trialId,
      Experiment experiment,
      BenchmarkSpec benchmarkSpec,
      int localPort,
      BenchmarkClass benchmarkClass) {
    // TODO(lukes): it would be nice to split this method into a few smaller more targeted methods
    Instrumentation instrumentation = experiment.instrumentation();
    Instrument instrument = instrumentation.instrument();
    WorkerSpec request = new WorkerSpec(
        trialId,
        instrumentation.workerClass(),
        instrumentation.workerOptions(),
        benchmarkSpec,
        ImmutableList.copyOf(instrumentation.benchmarkMethod.getParameterTypes()),
        localPort);

    ProcessBuilder processBuilder = new ProcessBuilder().redirectErrorStream(false);

    List<String> args = processBuilder.command();

    VirtualMachine vm = experiment.vm();
    VmConfig vmConfig = vm.config;
    args.addAll(getJvmArgs(vm, benchmarkClass));

    Iterable<String> instrumentJvmOptions = instrument.getExtraCommandLineArgs(vmConfig);
    logger.fine(String.format("Instrument(%s) Java args: %s", instrument.getClass().getName(),
        instrumentJvmOptions));
    Iterables.addAll(args, instrumentJvmOptions);

    // last to ensure that they're always applied
    args.addAll(vmConfig.workerProcessArgs());

    args.add(WorkerMain.class.getName());
    args.add(CommandLineSerializer.render(request));

    logger.finest(String.format("Full JVM (%s) args: %s", vm.name, args));
    return processBuilder;
  }
  
  @VisibleForTesting static List<String> getJvmArgs(
      VirtualMachine vm,
      BenchmarkClass benchmarkClass) {

    VmConfig vmConfig = vm.config;
    String platformName = vmConfig.platformName();

    List<String> args = Lists.newArrayList();
    String jdkPath = vmConfig.vmExecutable().getAbsolutePath();
    args.add(jdkPath);
    logger.fine(String.format("%s(%s) Path: %s", platformName, vm.name, jdkPath));

    ImmutableList<String> jvmOptions = vmConfig.options();
    args.addAll(jvmOptions);
    logger.fine(String.format("%s(%s) args: %s", platformName, vm.name, jvmOptions));
    
    ImmutableSet<String> benchmarkJvmOptions = benchmarkClass.vmOptions();
    args.addAll(benchmarkJvmOptions);
    logger.fine(String.format("Benchmark(%s) %s args: %s", benchmarkClass.name(), platformName,
        benchmarkJvmOptions));

    String classPath = vmConfig.workerClassPath();
    Collections.addAll(args, "-cp", classPath);
    logger.finer(String.format("Class path: %s", classPath));
    return args;
  }
}
