/*
 * Copyright (C) 2017 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */

package com.google.caliper.runner.target;

import com.google.caliper.runner.config.CaliperConfig;
import com.google.caliper.runner.config.VmConfig;
import com.google.caliper.runner.options.CaliperOptions;
import com.google.caliper.runner.platform.Platform;
import com.google.common.collect.ImmutableSet;
import dagger.Module;
import dagger.Provides;

/**
 * Module for binding the targets that Caliper should run the benchmark on.
 *
 * @author Colin Decker
 */
@Module
public abstract class TargetModule {

  @Provides
  static ImmutableSet<Target> provideTargets(
      CaliperOptions options, CaliperConfig config, Platform platform) {
    ImmutableSet<String> vmNames = options.vmNames();
    ImmutableSet.Builder<Target> builder = ImmutableSet.builder();
    if (vmNames.isEmpty()) {
      builder.add(Target.create("default", config.getDefaultVmConfig(platform)));
    } else {
      for (String vmName : vmNames) {
        VmConfig vmConfig = config.getVmConfig(platform, vmName);
        builder.add(Target.create(vmName, vmConfig));
      }
    }
    return builder.build();
  }
}
