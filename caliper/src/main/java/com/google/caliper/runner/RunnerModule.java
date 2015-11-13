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
import com.google.caliper.model.Run;
import com.google.caliper.options.CaliperOptions;
import com.google.caliper.platform.Platform;
import com.google.common.collect.ImmutableSet;

import dagger.Module;
import dagger.Provides;

import org.joda.time.Instant;

import java.util.UUID;

import javax.inject.Singleton;

/**
 * A Dagger module that configures bindings common to all {@link CaliperRun} implementations.
 */
// TODO(gak): throwing providers for all of the things that throw
@Module
final class RunnerModule {
  @Provides
  static ImmutableSet<VirtualMachine> provideVirtualMachines(
      CaliperOptions options,
      CaliperConfig config,
      Platform platform)
      throws InvalidConfigurationException {
    ImmutableSet<String> vmNames = options.vmNames();
    ImmutableSet.Builder<VirtualMachine> builder = ImmutableSet.builder();
    if (vmNames.isEmpty()) {
      builder.add(new VirtualMachine("default", config.getDefaultVmConfig(platform)));
    } else {
      for (String vmName : vmNames) {
        VmConfig vmConfig = config.getVmConfig(platform, vmName);
        builder.add(new VirtualMachine(vmName, vmConfig));
      }
    }
    return builder.build();
  }

  @Provides
  static Instant provideInstant() {
    return Instant.now();
  }

  @Provides static CaliperRun provideCaliperRun(ExperimentingCaliperRun experimentingCaliperRun) {
    return experimentingCaliperRun;
  }

  @Provides @Singleton
  static Run provideRun(UUID uuid, CaliperOptions caliperOptions, Instant startTime) {
    return new Run.Builder(uuid).label(caliperOptions.runName()).startTime(startTime).build();
  }
}
