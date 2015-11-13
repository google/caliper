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

  private static final String[] EMPTY_ARGS = new String[] {};

  private final String[] args;

  private boolean requireBenchmarkClassName;

  /**
   * Return a module that will provide access to configuration options and the name of the
   * benchmark class.
   *
   * @param args the arguments from which the configuration options and the benchmark class name
   *     are parsed; must have one non-option value that is the benchmark class name.
   */
  public static OptionsModule withBenchmarkClass(String [] args) {
    return new OptionsModule(args, true);
  }

  /**
   * Return a module that will provide access to configuration options without the name of the
   * benchmark class.
   *
   * @param args the arguments from which the configuration options are parsed; it must have no
   *     non-option values.
   */
  public static OptionsModule withoutBenchmarkClass(String [] args) {
    return new OptionsModule(args, false);
  }

  /**
   * Return a module that will provide access to the default configuration options.
   */
  public static OptionsModule defaultOptionsModule() {
    return new OptionsModule(EMPTY_ARGS, false);
  }

  public OptionsModule(String[] args, boolean requireBenchmarkClassName) {
    this.args = args.clone(); // defensive copy, just in case
    this.requireBenchmarkClassName = requireBenchmarkClassName;
  }

  @Provides
  @Singleton
  CaliperOptions provideOptions() throws InvalidCommandException {
    return ParsedOptions.from(args, requireBenchmarkClassName);
  }

  @Provides @CaliperDirectory static File provideCaliperDirectory(CaliperOptions options) {
    return options.caliperDirectory();
  }
}
