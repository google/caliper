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

package com.google.caliper.options;

import com.google.caliper.util.InvalidCommandException;
import dagger.Module;
import dagger.Provides;

import java.io.File;

import javax.inject.Singleton;

/**
 * Bindings for Caliper command line options.
 */
@Module
public final class OptionsModule {
  private final String[] args;

  public OptionsModule(String[] args) {
    this.args = args.clone(); // defensive copy, just in case
  }

  @Singleton
  @Provides CaliperOptions provideOptions() throws InvalidCommandException {
    // TODO(gak): throwing provider
    return ParsedOptions.from(args);
  }

  @Provides @CaliperDirectory static File provideCaliperDirectory(CaliperOptions options) {
    return options.caliperDirectory();
  }
}
