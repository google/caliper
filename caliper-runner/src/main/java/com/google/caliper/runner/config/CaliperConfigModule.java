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
    // TODO(kevinb): deal with migration issue from old-style .caliperrc
    loggingConfigLoader.loadLoggingConfig();

    ImmutableMap<String, String> defaults = loadDefaults();
    ImmutableMap<String, String> configProperties = caliperOptions.configProperties();
    File configFile = caliperOptions.caliperConfigFile();
    if (configFile.exists()) {
      try {
        return loadCaliperConfig(Files.asByteSource(configFile), configProperties, defaults);
      } catch (IOException keepGoing) {
      }
    }

    ByteSource supplier = Util.resourceSupplier(CaliperConfig.class, "default-config.properties");
    tryCopyIfNeeded(supplier, configFile);

    try {
      return loadCaliperConfig(supplier, configProperties, defaults);
    } catch (IOException e) {
      throw new AssertionError(e); // class path must be messed up
    }
  }

  private static ImmutableMap<String, String> loadDefaults() {
    try {
      return Util.loadProperties(
          Util.resourceSupplier(CaliperConfig.class, "global-config.properties"));
    } catch (IOException impossible) {
      throw new AssertionError(impossible);
    }
  }

  private static CaliperConfig loadCaliperConfig(
      ByteSource source,
      ImmutableMap<String, String> configProperties,
      ImmutableMap<String, String> defaults)
      throws IOException {
    return new CaliperConfig(
        mergeProperties(configProperties, Util.loadProperties(source), defaults));
  }

  private static ImmutableMap<String, String> mergeProperties(
      Map<String, String> commandLine, Map<String, String> user, Map<String, String> defaults) {
    Map<String, String> map = Maps.newHashMap(defaults);
    map.putAll(user); // overwrite and augment
    map.putAll(commandLine); // overwrite and augment
    Iterables.removeIf(map.values(), Predicates.equalTo(""));
    return ImmutableMap.copyOf(map);
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
