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

import com.google.caliper.config.CaliperConfig;
import com.google.caliper.config.InvalidConfigurationException;
import com.google.caliper.config.VmConfig;
import com.google.caliper.options.CaliperOptions;
import com.google.common.collect.ImmutableSet;
import com.google.inject.AbstractModule;
import com.google.inject.Provides;

/**
 * A Guice module that configures bindings common to all {@link CaliperRun} implementations. Callers
 * shouldn't install this directly.  Use either {@link PerflabInfoRunnerModule} or
 * {@link ExperimentingRunnerModule}.
 */
// TODO(gak): throwing providers for all of the things that throw
final class RunnerModule extends AbstractModule {
  @Override protected void configure() {
    requireBinding(CaliperOptions.class);
    requireBinding(CaliperConfig.class);
  }

  @Provides ImmutableSet<VirtualMachine> provideVirtualMachines(CaliperOptions options,
      CaliperConfig config) throws InvalidConfigurationException {
    ImmutableSet<String> vmNames = options.vmNames();
    ImmutableSet.Builder<VirtualMachine> builder = ImmutableSet.builder();
    if (vmNames.isEmpty()) {
      builder.add(new VirtualMachine("default", config.getDefaultVmConfig()));
    } else {
      for (String vmName : vmNames) {
        VmConfig vmConfig = config.getVmConfig(vmName);
        builder.add(new VirtualMachine(vmName, vmConfig));
      }
    }
    return builder.build();
  }
}
