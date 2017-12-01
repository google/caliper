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
import com.google.caliper.bridge.WorkerRequest;
import com.google.caliper.runner.config.VmConfig;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import java.util.List;
import java.util.logging.Logger;

/**
 * Factory for building a {@link Command} for starting a worker.
 *
 * @author Colin Decker
 */
final class WorkerCommandFactory {

  /*
   * Note: this could be replaced with an @Provides method in a module, but there will be
   * significant changes to it soon anyway (building the command should probably be the
   * responsibility of the "worker spec" (e.g. device + VM + options + request), so it seemed
   * simplest to just leave it as a standalone class for now.
   */

  private static final Logger logger = Logger.getLogger(WorkerCommandFactory.class.getName());

  /** Builds a command that can be used to start a worker. */
  static Command buildCommand(
      Experiment experiment, BenchmarkClass benchmarkClass, WorkerRequest request) {
    // TODO(lukes): it would be nice to split this method into a few smaller more targeted methods
    Instrument instrument = experiment.instrumentedMethod().instrument();
    Command.Builder builder = Command.builder();

    Target target = experiment.target();
    VmConfig vm = target.vm();

    builder.putAllEnvironmentVariables(target.platform().workerEnvironment());

    builder.addArguments(getJvmArgs(target, benchmarkClass));

    Iterable<String> instrumentJvmOptions = instrument.getExtraCommandLineArgs(vm);
    logger.fine(
        String.format(
            "Instrument(%s) Java args: %s", instrument.getClass().getName(), instrumentJvmOptions));
    builder.addArguments(instrumentJvmOptions);

    // last to ensure that they're always applied
    builder.addArguments(vm.workerProcessArgs());

    builder.addArgument("com.google.caliper.worker.WorkerMain");
    builder.addArgument(CommandLineSerializer.render(request));

    Command command = builder.build();
    logger.finest(String.format("Full JVM (%s) args: %s", target.name(), command.arguments()));
    return command;
  }

  @VisibleForTesting
  static List<String> getJvmArgs(Target target, BenchmarkClass benchmarkClass) {
    VmConfig vm = target.vm();
    String platformName = vm.platform().name();

    List<String> args = Lists.newArrayList();
    String jdkPath = vm.vmExecutable().getAbsolutePath();
    args.add(jdkPath);
    logger.fine(String.format("%s(%s) Path: %s", platformName, target.name(), jdkPath));

    ImmutableList<String> jvmOptions = vm.options();
    args.addAll(jvmOptions);
    logger.fine(String.format("%s(%s) args: %s", platformName, target.name(), jvmOptions));

    ImmutableSet<String> benchmarkJvmOptions = benchmarkClass.vmOptions();
    args.addAll(benchmarkJvmOptions);
    logger.fine(
        String.format(
            "Benchmark(%s) %s args: %s", benchmarkClass.name(), platformName, benchmarkJvmOptions));

    ImmutableList<String> classPathArgs = vm.workerClassPathArgs();
    args.addAll(classPathArgs);
    logger.finer(String.format("Class path args: %s", Joiner.on(' ').join(classPathArgs)));
    return args;
  }
}
