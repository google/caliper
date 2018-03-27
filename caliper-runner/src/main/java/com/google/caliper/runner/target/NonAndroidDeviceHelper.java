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

import static java.lang.Thread.currentThread;

import com.google.caliper.runner.config.InvalidConfigurationException;
import com.google.caliper.runner.config.VmConfig;
import com.google.caliper.runner.config.VmType;
import com.google.common.base.Predicate;
import com.google.common.collect.Collections2;
import java.io.File;
import java.lang.management.ManagementFactory;
import java.util.Collection;
import java.util.Map;

/** Helper for when the local device is <i>not</i> an Android device. */
final class NonAndroidDeviceHelper implements LocalDevice.Helper {

  @Override
  public void setUp() {}

  @Override
  public VmType defaultVmType() {
    return VmType.JVM;
  }

  @Override
  public void configureDefaultVm(VmConfig.Builder builder) {
    builder.home(System.getProperty("java.home")).addAllArgs(jvmInputArguments());
  }

  /**
   * Predicate for filtering out JVM flags (from those used to start the runner JVM) that we don't
   * want to pass on to worker VMs.
   */
  private static final Predicate<String> JVM_FLAGS_TO_RETAIN =
      new Predicate<String>() {
        @Override
        public boolean apply(String flag) {
          // Exclude the -agentlib:jdwp param which configures the socket debugging protocol.
          // If this is set in the parent VM we do not want it to be inherited by the child
          // VM.  If it is, the child will die immediately on startup because it will fail to
          // bind to the debug port (because the parent VM is already bound to it).
          return !flag.startsWith("-agentlib:jdwp");
        }
      };

  /**
   * Returns the set of VM args used when starting this process that we want to pass on to worker
   * VMs.
   */
  private static Collection<String> jvmInputArguments() {
    // TODO(cgdecker): Don't pass any input args to workers by default.
    return Collections2.filter(
        ManagementFactory.getRuntimeMXBean().getInputArguments(), JVM_FLAGS_TO_RETAIN);
  }

  @Override
  public File getHomeDir(Vm vm, File baseDirectory) {
    return new File(baseDirectory, vm.name());
  }

  @Override
  public String getWorkerClasspath(VmType type) {
    if (type.equals(VmType.ANDROID)) {
      throw new InvalidConfigurationException(
          "android VMs not supported on non-android devices yet");
    }

    // Use the effective class path in case this is being invoked in an isolated class loader
    String classpath =
        EffectiveClassPath.getClassPathForClassLoader(currentThread().getContextClassLoader());
    return classpath;
  }

  @Override
  public void addToWorkerProcessEnvironment(Map<String, String> env) {}
}
