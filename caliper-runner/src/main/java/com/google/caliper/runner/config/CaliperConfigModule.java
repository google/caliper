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

import static com.google.common.base.Predicates.contains;

import com.google.caliper.runner.options.CaliperOptions;
import com.google.caliper.util.Stdout;
import com.google.caliper.util.Util;
import com.google.common.base.Joiner;
import com.google.common.base.Joiner.MapJoiner;
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
import java.io.PrintWriter;
import java.util.Map;
import java.util.logging.LogManager;
import java.util.regex.Pattern;
import javax.inject.Singleton;

/** Provides {@link CaliperConfig}. */
@Module
public abstract class CaliperConfigModule {

  private CaliperConfigModule() {}

  private static final MapJoiner PROPERTIES_JOINER = Joiner.on("\n").withKeyValueSeparator("=");
  private static final Pattern RESULTS_UPLOAD_PATTERN = Pattern.compile("^results\\.upload\\..+");

  private static ImmutableMap<String, String> filterProperties(
      String sourceDescription, ImmutableMap<String, String> properties, PrintWriter stdout) {
    return logAndRemoveUploadProperties(sourceDescription, properties, stdout);
  }

  private static ImmutableMap<String, String> logAndRemoveUploadProperties(
      String sourceDescription, ImmutableMap<String, String> properties, PrintWriter stdout) {
    // Previously existing user configuration files may have results.upload entries for which the
    // implementation class will no longer exist. Warn about this and suggest that the user remove
    // those properties. Other things we could do include:
    // - Throwing an exception, but that feels too heavy-handed perhaps.
    // - Removing results.upload entries from the user's configuration for them, but that seems
    //   overly invasive and likely that the user would miss the message.

    ImmutableMap<String, String> uploadProperties =
        ImmutableMap.copyOf(Maps.filterKeys(properties, contains(RESULTS_UPLOAD_PATTERN)));
    if (uploadProperties.isEmpty()) {
      return properties;
    }

    stdout.printf(
        "The Caliper webapp has been shut down and results may no longer be uploaded to it."
            + "%n%n"
            + "The following configuration options are being ignored:%n"
            + "%s"
            + "%n%n"
            + "Please remove any configuration options starting with 'results.upload' from %s. "
            + "If you want to use a custom ResultProcessor implementation to upload results to "
            + "another location, please use a configuration key other than 'upload' for it.%n%n",
        PROPERTIES_JOINER.join(uploadProperties), sourceDescription);

    return ImmutableMap.copyOf(Maps.difference(properties, uploadProperties).entriesOnlyOnLeft());
  }

  @Provides
  @Singleton
  static CaliperConfig caliperConfig(
      CaliperOptions caliperOptions,
      LoggingConfigLoader loggingConfigLoader,
      @Stdout PrintWriter stdout) {
    loggingConfigLoader.loadLoggingConfig();

    // First get the non-device-specific global config (global-config.properties), user config
    // (~/.caliper/config.properties or whatever the user specified on the command line) and
    // command line config (supplied with "-Cproperty.key=value").
    ImmutableMap<String, String> globalConfig = loadGlobalConfig();
    ImmutableMap<String, String> userConfig = loadUserConfig(caliperOptions, stdout);
    ImmutableMap<String, String> commandLineConfig =
        filterProperties("command line options", caliperOptions.configProperties(), stdout);

    // Create a CaliperConfig using just those options. They should contain all the information we
    // need to get get the type of device this run is targeting.
    CaliperConfig config = merge(globalConfig, userConfig, commandLineConfig);
    DeviceType deviceType = config.getDeviceConfig(caliperOptions).type();

    // Get the global and user configs for the device type
    ImmutableMap<String, String> globalDeviceTypeConfig = loadGlobalConfig("-" + deviceType);
    ImmutableMap<String, String> userDeviceTypeConfig =
        loadUserConfig(caliperOptions, "-" + deviceType, stdout);

    return merge(
        globalConfig, globalDeviceTypeConfig, userConfig, userDeviceTypeConfig, commandLineConfig);
  }

  private static ImmutableMap<String, String> loadUserConfig(
      CaliperOptions caliperOptions, PrintWriter stdout) {
    File configFile = caliperOptions.caliperConfigFile();
    if (configFile.exists()) {
      try {
        return filterProperties(
            "file " + configFile, Util.loadProperties(Files.asByteSource(configFile)), stdout);
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

  private static ImmutableMap<String, String> loadUserConfig(
      CaliperOptions caliperOptions, String suffix, PrintWriter stdout) {
    File mainConfigFile = caliperOptions.caliperConfigFile().getAbsoluteFile();

    String mainConfigFileBaseName = Files.getNameWithoutExtension(mainConfigFile.getName());
    String configFileName = mainConfigFileBaseName + suffix + ".properties";

    File parentDir = mainConfigFile.getParentFile();
    File configFile;
    if (parentDir == null) {
      // unlikely since we got an absolute path, but try using just the file name
      configFile = new File(configFileName);
    } else {
      configFile = new File(parentDir, configFileName);
    }

    if (configFile.exists()) {
      try {
        return filterProperties(
            "file " + configFile, Util.loadProperties(Files.asByteSource(configFile)), stdout);
      } catch (IOException e) {
        throw new InvalidConfigurationException("Couldn't load config file: " + configFile, e);
      }
    }

    // For device-type specific user config, don't create the file automatically if it doesn't exist
    return ImmutableMap.of();
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
          Util.resourceSupplier(CaliperConfig.class, "global-config" + suffix + ".properties"));
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
