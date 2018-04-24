/*
 * Copyright (C) 2011 Google Inc.
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

package com.google.caliper.runner.config;

import com.google.caliper.runner.options.CaliperOptions;
import com.google.caliper.util.Util;
import com.google.common.base.Predicates;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Iterables;
import com.google.common.collect.Maps;
import com.google.common.io.ByteSource;
import com.google.common.io.Files;
import dagger.Module;
import dagger.Provides;
import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.logging.LogManager;
import javax.inject.Singleton;

/** Provides {@link CaliperConfig}. */
@Module
public abstract class CaliperConfigModule {
  private CaliperConfigModule() {}

  @Provides
  @Singleton
  static CaliperConfig caliperConfig(
      CaliperOptions caliperOptions, LoggingConfigLoader loggingConfigLoader) {
    loggingConfigLoader.loadLoggingConfig();

    // First get the non-device-specific global config (global-config.properties), user config
    // (~/.caliper/config.properties or whatever the user specified on the command line) and
    // command line config (supplied with "-Cproperty.key=value").
    ImmutableMap<String, String> globalConfig = loadGlobalConfig();
    ImmutableMap<String, String> userConfig = getUserConfig(caliperOptions);
    ImmutableMap<String, String> commandLineConfig = caliperOptions.configProperties();

    // Create a CaliperConfig using just those options. They should contain all the information we
    // need to get get the type of device this run is targeting.
    CaliperConfig config = merge(globalConfig, userConfig, commandLineConfig);
    DeviceType deviceType = config.getDeviceConfig(caliperOptions).type();

    // Get the global config for the device type (e.g. "global-config-adb.properties").
    ImmutableMap<String, String> globalDeviceTypeConfig = loadGlobalConfig("-" + deviceType);

    // Return the CaliperConfig using that global config to override the base global config.
    // TODO(cgdecker): It's possible there should be be device-type-specific user configs as well,
    // particularly since the default user config sets -Xms3g -Xmx3g, which really isn't going to
    // work for adb devices in general. But it's not clear what we should do in the case where the
    // user supplied a config file on the command line rather than using the default. Assume that
    // config file has everything they want specified?
    return merge(globalConfig, globalDeviceTypeConfig, userConfig, commandLineConfig);
  }

  private static ImmutableMap<String, String> getUserConfig(CaliperOptions caliperOptions) {
    File configFile = caliperOptions.caliperConfigFile();
    if (configFile.exists()) {
      try {
        return Util.loadProperties(Files.asByteSource(configFile));
      } catch (IOException keepGoing) {
      }
    }

    ByteSource supplier = Util.resourceSupplier(CaliperConfig.class, "default-config.properties");
    tryCopyIfNeeded(supplier, configFile);

    try {
      return Util.loadProperties(supplier);
    } catch (IOException e) {
      throw new AssertionError(e);
    }
  }

  private static CaliperConfig merge(ImmutableMap<String, String>... maps) {
    Map<String, String> result = Maps.newHashMap();
    for (Map<String, String> map : maps) {
      result.putAll(map);
    }
    Iterables.removeIf(result.values(), Predicates.equalTo(""));
    return new CaliperConfig(ImmutableMap.copyOf(result));
  }

  private static ImmutableMap<String, String> loadGlobalConfig() {
    return loadGlobalConfig("");
  }

  private static ImmutableMap<String, String> loadGlobalConfig(String suffix) {
    try {
      return Util.loadProperties(
          Util.resourceSupplier(
              CaliperConfig.class, "global-config" + suffix + ".properties"));
    } catch (IOException impossible) {
      throw new AssertionError(impossible);
    }
  }

  private static void tryCopyIfNeeded(ByteSource supplier, File rcFile) {
    if (!rcFile.exists()) {
      try {
        supplier.copyTo(Files.asByteSink(rcFile));
      } catch (IOException e) {
        rcFile.delete();
      }
    }
  }

  @Provides
  static LogManager provideLogManager() {
    return LogManager.getLogManager();
  }
}
