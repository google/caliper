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

package com.google.caliper.config;

import dagger.Module;
import dagger.Provides;

import java.util.logging.LogManager;
import javax.inject.Singleton;

/**
 * Bindings for Caliper configuration.
 */
@Module
public final class ConfigModule {

  /**
   * The {@code doNotRemove} parameter is required here to ensure that the logging configuration
   * is loaded and used to update the logger settings before the configuration is loaded.
   */
  @Provides @Singleton
  static CaliperConfig provideCaliperConfig(
      CaliperConfigLoader configLoader,
      @SuppressWarnings("unused") LoggingConfigLoader doNotRemove)
      throws InvalidConfigurationException {

    return configLoader.loadOrCreate();
  }

  @Provides static LogManager provideLogManager() {
    return LogManager.getLogManager();
  }
}
