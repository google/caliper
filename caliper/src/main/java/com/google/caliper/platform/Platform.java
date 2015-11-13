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

package com.google.caliper.platform;

import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.base.Predicate;
import com.google.common.collect.ImmutableSet;

import java.io.File;
import java.util.Collection;
import java.util.Map;

/**
 * An abstraction of the platform within which caliper (both the scheduler and the actual workers)
 * will run.
 */
public abstract class Platform {

  private final Platform.Type platformType;

  public Platform(Type platformType) {
    this.platformType = checkNotNull(platformType);
  }

  /**
   * Get the executable for the virtual machine for this platform.
   *
   * @param vmHome the home directory of the virtual machine, allows testing across multiple vms on
   *     the same platform in one go.
   */
  public abstract File vmExecutable(File vmHome);

  /**
   * Additional virtual machine arguments common to all instruments that are passed to a worker.
   */
  public abstract ImmutableSet<String> commonInstrumentVmArgs();

  /**
   * The name of the platform type.
   */
  public String name() {
    return platformType.name;
  }

  /**
   * Additional arguments that should be passed to a worker.
   */
  public abstract ImmutableSet<String> workerProcessArgs();

  /**
   * The class path that should be used to run a worker..
   */
  public abstract String workerClassPath();

  /**
   * Checks to see whether the specific class is supported on this platform.
   *
   * <p>This checks to see whether {@link SupportedPlatform} specifies a {@link Type} that
   * matches this platform.
   *
   * @param clazz the class to check.
   * @return true if it is supported, false otherwise.
   */
  public boolean supports(Class<?> clazz) {
    SupportedPlatform annotation = clazz.getAnnotation(SupportedPlatform.class);
    if (annotation == null) {
      // Support must be explicitly declared.
      return false;
    }

    Platform.Type[] types = annotation.value();
    for (Type type : types) {
      if (type.equals(platformType)) {
        return true;
      }
    }

    return false;
  }

  /**
   * Get the input arguments for the current running JVM.
   */
  public abstract Collection<String> inputArguments();

  /**
   * Selects the names of properties that will be used to populate the
   * {@link com.google.caliper.model.VmSpec} for a specific run.
   */
  public abstract Predicate<String> vmPropertiesToRetain();

  /**
   * Checks that the vm options are appropriate for this platform, throws an exception if they are
   * not.
   */
  public abstract void checkVmProperties(Map<String, String> options);

  /**
   * Get the default vm home directory.
   */
  public File defaultVmHomeDir() {
    // Currently both supported platforms use java.home property to refer to the 'home' directory
    // of the vm, in the case of Android it is the directory containing the dalvikvm executable.
    return new File(System.getProperty("java.home"));
  }

  /**
   * Get the home directory of a custom virtual machine.
   * @param vmGroupMap the configuration properties for all VMs, may contain default properties that
   *     apply to all VMs.
   * @param vmConfigName the name of the VM within the configuration, used to access VM specific
   *     properties from the {@code vmGroupMap}.
   * @throws VirtualMachineException if there was a problem with the VM, either the configuration
   *     or the file system.
   */
  public abstract File customVmHomeDir(Map<String, String> vmGroupMap, String vmConfigName)
          throws VirtualMachineException;

  /**
   * The type of platforms supported.
   */
  public enum Type {
    DALVIK("Dalvik"),
    JVM("Java");

    private final String name;

    Type(String name) {
      this.name = name;
    }
  }
}
