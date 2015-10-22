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

import com.google.caliper.options.CaliperOptions;
import com.google.caliper.util.InvalidCommandException;
import com.google.caliper.util.Util;
import dagger.Module;
import dagger.Provides;
import javax.inject.Singleton;

/**
 * Provides bindings to integrate other modules into the {@link MainComponent}.
 */
@Module
class MainModule {

  private static Class<?> benchmarkClassForName(String className)
      throws InvalidCommandException, UserCodeException {
    try {
      return Util.lenientClassForName(className);
    } catch (ClassNotFoundException e) {
      throw new InvalidCommandException("Benchmark class not found: " + className);
    } catch (ExceptionInInitializerError e) {
      throw new UserCodeException(
          "Exception thrown while initializing class '" + className + "'", e.getCause());
    } catch (NoClassDefFoundError e) {
      throw new UserCodeException("Unable to load " + className, e);
    }
  }

  @Provides
  @Singleton
  @Running.BenchmarkClass
  static Class<?> provideBenchmarkClass(CaliperOptions options) {
    return benchmarkClassForName(options.benchmarkClassName());
  }
}
