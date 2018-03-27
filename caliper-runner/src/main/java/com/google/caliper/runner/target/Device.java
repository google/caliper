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

import static com.google.common.base.Preconditions.checkNotNull;

import com.google.caliper.runner.config.DeviceConfig;
import com.google.caliper.runner.config.VmConfig;
import com.google.caliper.runner.config.VmType;
import com.google.common.util.concurrent.AbstractIdleService;
import com.google.common.util.concurrent.Service;

/**
 * A {@link Service} for handling communication with a device of a particular type and running VMs
 * on the device.
 *
 * <p>Starting the service should perform any setup needed to be able to run worker processes on the
 * device, including (but not necessarily limited to) things like establishing a connection with the
 * device and transferring classpath files needed to run a worker to the device. Stopping the
 * service should clean up files created on the device as necessary and close any connections.
 */
public abstract class Device extends AbstractIdleService {

  private final DeviceConfig config;
  private final ShutdownHookRegistrar shutdownHookRegistrar;

  protected Device(DeviceConfig config, ShutdownHookRegistrar shutdownHookRegistrar) {
    this.config = checkNotNull(config);
    this.shutdownHookRegistrar = checkNotNull(shutdownHookRegistrar);
  }

  /** Returns the configuration for this device. */
  public final DeviceConfig config() {
    return config;
  }

  /** Returns the name to use for this device. */
  public final String name() {
    return config.name();
  }

  /** Creates a target for this device with its default VM configuration. */
  public final Target createDefaultTarget() {
    return createTarget(defaultVmConfig());
  }

  /** Creates a {@link Target} for the given VM configuration on this device. */
  public final Target createTarget(VmConfig vmConfig) {
    return Target.create(this, createVm(vmConfig));
  }

  /** Creates a VM for the given configuration. */
  private Vm createVm(VmConfig vmConfig) {
    VmType type = vmConfig.type().or(defaultVmType());
    String classpath = workerClasspath(type);
    switch (type) {
      case JVM:
        return new Jvm(vmConfig, classpath);
      case ANDROID:
        return new AndroidVm(vmConfig, classpath);
    }
    throw new AssertionError(type);
  }

  /** Returns the absolute path to the executable for the given VM on this device. */
  public abstract String vmExecutablePath(Vm vm);

  /** Returns the classpath to use with the given VM. */
  public abstract String workerClasspath(VmType type);

  /** Returns the default type for VMs on this device that don't specify a type. */
  public abstract VmType defaultVmType();

  /** Returns the VM configuration to use for this device when the user doesn't specify one. */
  public abstract VmConfig defaultVmConfig();

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
