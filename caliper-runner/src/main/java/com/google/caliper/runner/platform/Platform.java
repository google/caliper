/*
 * Copyright (C) 2015 Google Inc.
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

package com.google.caliper.runner.platform;

import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.base.Predicate;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import java.io.File;
import java.util.Collection;
import java.util.Map;

/**
 * An abstraction of the platform within which caliper (both the scheduler and the actual workers)
 * will run.
 */
public abstract class Platform {

  private final VmType vmType;

  public Platform(VmType vmType) {
    this.vmType = checkNotNull(vmType);
  }

  /**
   * Get the executable for the virtual machine for this platform.
   *
   * @param vmHome the home directory of the virtual machine, allows testing across multiple vms on
   *     the same platform in one go.
   */
  public abstract File vmExecutable(File vmHome);

  /** Additional virtual machine arguments common to all instruments that are passed to a worker. */
  public abstract ImmutableSet<String> commonInstrumentVmArgs();

  /** The name of the platform type. */
  public String name() {
    return vmType.toString();
  }

  /** Returns the VM type for this platform. */
  public VmType vmType() {
    return vmType;
  }

  /** Additional arguments that should be passed to a worker. */
  public abstract ImmutableSet<String> workerProcessArgs();

  /** The class path that should be used to run a worker. */
  protected abstract String workerClassPath();

  /**
   * The set of arguments that specify the classpath with is passed to the worker.
   *
   * <p>By default, this is {@code -cp} followed by the {@code workerClassPath()}.
   */
  public ImmutableList<String> workerClassPathArgs() {
    return ImmutableList.of("-cp", workerClassPath());
  }

  /**
   * Checks to see whether the specific class is supported on this platform.
   *
   * <p>This checks to see whether {@link SupportsVmType} specifies a {@link VmType} that matches
   * this platform.
   *
   * @param clazz the class to check.
   * @return true if it is supported, false otherwise.
   */
  public boolean supports(Class<?> clazz) {
    return vmType.supports(clazz);
  }

  /** Get the input arguments for the current running JVM. */
  public abstract Collection<String> inputArguments();

  /**
   * Selects the names of properties that will be used to populate the {@link
   * com.google.caliper.model.VmSpec} for a specific run.
   */
  public abstract Predicate<String> vmPropertiesToRetain();

  /**
   * Checks that the vm options are appropriate for this platform, throws an exception if they are
   * not.
   */
  public abstract void checkVmProperties(Map<String, String> options);

  /**
   * Returns a map of key/value pairs to add to the environment when starting a worker. If the
   * env already contains values for any of the keys in the returned map, those values will be
   * overwritten with the new values from the map.
   */
  public ImmutableMap<String, String> workerEnvironment() {
    return ImmutableMap.of();
  }

  /** Get the default vm home directory. */
  public File defaultVmHomeDir() {
    return new File(System.getProperty("java.home"));
  }

  /**
   * Get the home directory of a custom virtual machine.
   *
   * @param vmGroupMap the configuration properties for all VMs, may contain default properties that
   *     apply to all VMs.
   * @param vmConfigName the name of the VM within the configuration, used to access VM specific
   *     properties from the {@code vmGroupMap}.
   * @throws VirtualMachineException if there was a problem with the VM, either the configuration or
   *     the file system.
   */
  public abstract File customVmHomeDir(Map<String, String> vmGroupMap, String vmConfigName)
      throws VirtualMachineException;
}
