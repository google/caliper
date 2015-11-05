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

import com.google.caliper.options.CaliperOptions;
import com.google.inject.AbstractModule;
import com.google.inject.Provides;

import java.util.logging.LogManager;

/**
 * Bindings for Caliper configuration.
 */
public final class ConfigModule extends AbstractModule {
  @Override protected void configure() {
    requireBinding(CaliperOptions.class);
    bind(LoggingConfigLoader.class).asEagerSingleton();
  }

  @Provides CaliperConfig provideCaliperConfig(CaliperConfigLoader configLoader)
      throws InvalidConfigurationException {
    return configLoader.loadOrCreate();
  }

  @Provides LogManager provideLogManager() {
    return LogManager.getLogManager();
  }
}
