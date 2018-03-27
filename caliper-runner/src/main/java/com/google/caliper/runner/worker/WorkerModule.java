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
import com.google.caliper.runner.config.VmConfig;
import com.google.caliper.runner.server.ServerSocketService;
import com.google.caliper.runner.target.Target;
import com.google.caliper.runner.target.Vm;
import com.google.common.util.concurrent.ListenableFuture;
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
  private WorkerModule() {}

  @WorkerScoped
  @Provides
  static ListenableFuture<OpenedSocket> provideWorkerSocket(
      WorkerSpec spec, ServerSocketService serverSocketService) {
    return serverSocketService.getConnection(spec.id());
  }

  @Provides
  static Vm provideVm(Target target) {
    return target.vm();
  }

  @Provides
  static VmConfig provideVmConfig(Vm vm) {
    return vm.config();
  }
}
