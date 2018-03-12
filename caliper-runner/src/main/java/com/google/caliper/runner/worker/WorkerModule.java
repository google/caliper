/*
 * Copyright (C) 2017 Google Inc.
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

package com.google.caliper.runner.worker;

import com.google.caliper.bridge.OpenedSocket;
import com.google.caliper.core.Running.BenchmarkClass;
import com.google.caliper.runner.config.VmConfig;
import com.google.caliper.runner.server.LocalPort;
import com.google.caliper.runner.server.ServerSocketService;
import com.google.caliper.runner.target.Target;
import com.google.common.util.concurrent.ListenableFuture;
import dagger.Binds;
import dagger.Module;
import dagger.Provides;

/**
 * Module for creating a {@link Worker}. A {@link Target} and {@link WorkerSpec} must both be bound
 * in the same context.
 *
 * @author Colin Decker
 */
@Module
public abstract class WorkerModule {

  @WorkerScoped
  @Provides
  static ListenableFuture<OpenedSocket> provideWorkerSocket(
      WorkerSpec spec, ServerSocketService serverSocketService) {
    return serverSocketService.getConnection(spec.id());
  }

  // TODO(cgdecker): This will need to be bound based on the device in the future.
  @Binds
  abstract WorkerStarter bindWorkerStarter(LocalWorkerStarter workerStarter);

  @Provides
  static Command provideWorkerCommand(
      Target target,
      WorkerSpec spec,
      @LocalPort int port,
      @BenchmarkClass String benchmarkClassName) {
    VmConfig vm = target.vm();
    return Command.builder()
        .putAllEnvironmentVariables(target.platform().workerEnvironment())
        .addArgument(vm.vmExecutable().getAbsolutePath())
        .addArguments(vm.options())
        .addArguments(spec.vmOptions(vm))
        .addArguments(vm.workerClassPathArgs())
        .addArguments(vm.workerProcessArgs())
        .addArgument(spec.mainClass())
        .addArgument(spec.id())
        .addArgument(port)
        .addArgument(benchmarkClassName)
        .build();
  }
}
