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
import com.google.caliper.runner.config.VmType;
import com.google.caliper.runner.options.CaliperOptions;
import com.google.common.collect.ImmutableSet;
import dagger.Module;
import dagger.Provides;
import javax.inject.Singleton;

/**
 * Module for binding the targets that Caliper should run the benchmark on.
 *
 * @author Colin Decker
 */
@Module
public abstract class TargetModule {
  private TargetModule() {}

  @Singleton
  @Provides
  static ImmutableSet<Target> provideTargets(
      Device device, CaliperOptions options, CaliperConfig config) {
    ImmutableSet<String> vmNames = options.vmNames();
    if (vmNames.isEmpty()) {
      return ImmutableSet.of(device.createDefaultTarget());
    }

    ImmutableSet.Builder<Target> builder = ImmutableSet.builder();
    for (String vmName : vmNames) {
      builder.add(device.createTarget(config.getVmConfig(vmName)));
    }
    return builder.build();
  }

  @Provides
  static ImmutableSet<VmType> vmTypes(ImmutableSet<Target> targets) {
    ImmutableSet.Builder<VmType> builder = ImmutableSet.builder();
    for (Target target : targets) {
      builder.add(target.vm().type());
    }
    return builder.build();
  }
}
