/*
 * Copyright (C) 2018 Google Inc.
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

import com.google.common.util.concurrent.AbstractIdleService;

/**
 * A {@link Service} for handling communication with a device of a particular type and running VMs
 * on the device.
 *
 * <p>Starting the service should perform any setup needed to be able to run worker processes on the
 * device, including (but not necessarily limited to) things like establishing a connection with the
 * device and transferring classpath files needed to run a worker to the device. Stopping the
 * service should clean up files created on the device as necessary and close any connections.
 */
public abstract class DeviceService extends AbstractIdleService {

  private final ShutdownHookRegistrar shutdownHookRegistrar;

  protected DeviceService(ShutdownHookRegistrar shutdownHookRegistrar) {
    this.shutdownHookRegistrar = shutdownHookRegistrar;
  }

  /** Starts a process on the device to run a VM using the given VM process spec. */
  public final VmProcess startVm(VmProcess.Spec spec, VmProcess.Logger logger) throws Exception {
    final VmProcess process = doStartVm(spec, logger);
    final Thread shutdownHook =
        new Thread("worker-shutdown-hook-" + spec.id()) {
          @Override
          public void run() {
            process.kill();
          }
        };
    process.addStopListener(
        new VmProcess.StopListener() {
          @Override
          public void stopped(VmProcess process) {
            shutdownHookRegistrar.removeShutdownHook(shutdownHook);
          }
        });
    shutdownHookRegistrar.addShutdownHook(shutdownHook);
    return process;
  }

  /** Implements {@link #startVm}. */
  protected abstract VmProcess doStartVm(VmProcess.Spec spec, VmProcess.Logger logger)
      throws Exception;
}
