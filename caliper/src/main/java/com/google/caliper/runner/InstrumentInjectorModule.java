/*
 * Copyright (C) 2015 Google Inc.
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

import com.google.caliper.config.InstrumentConfig;
import com.google.common.collect.ImmutableMap;
import dagger.Module;
import dagger.Provides;

/**
 * The set of bindings available for injecting into {@link Instrument} instances.
 */
@Module
final class InstrumentInjectorModule {

  private final InstrumentConfig instrumentConfig;

  private final String instrumentName;

  InstrumentInjectorModule(InstrumentConfig instrumentConfig, String instrumentName) {
    this.instrumentConfig = instrumentConfig;
    this.instrumentName = instrumentName;
  }

  @Provides
  InstrumentConfig provideInstrumentConfig() {
    return instrumentConfig;
  }

  @Provides
  @InstrumentOptions
  static ImmutableMap<String, String> provideInstrumentOptions(InstrumentConfig config) {
    return config.options();
  }

  @Provides
  @InstrumentName
  String provideInstrumentName() {
    return instrumentName;
  }
}
