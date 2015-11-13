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

import com.google.caliper.config.ConfigModule;
import com.google.caliper.options.OptionsModule;
import com.google.caliper.util.OutputModule;
import com.google.common.collect.ImmutableSet;

import dagger.Component;

import java.io.PrintWriter;
import java.lang.reflect.Method;
import java.util.List;

import javax.inject.Singleton;

/**
 * Determines whether a class contains one or more benchmarks or not.
 *
 * <p>Useful for tools that need to check whether a class contains benchmarks before running them
 * by calling an appropriate method on {@link CaliperMain}.
 */
// This should be considered part of the public API alongside {@link CaliperMain}.
public final class BenchmarkClassChecker {

  /**
   * Create a new instance of {@link BenchmarkClassChecker}.
   *
   * @param arguments a list of command line arguments for Caliper, can include any of the
   *     options supported by Caliper.
   * @return a new instance of {@link BenchmarkClassChecker}.
   */
  public static BenchmarkClassChecker create(List<String> arguments) {
    return new BenchmarkClassChecker(arguments);
  }

  /**
   * The set of {@link Instrument instruments} that are used to determine whether a class has any
   * methods suitable for benchmarking.
   */
  private final ImmutableSet<Instrument> instruments;

  private BenchmarkClassChecker(List<String> arguments) {
    String[] args = arguments.toArray(new String[arguments.size()]);
    InstrumentProvider instrumentProvider = DaggerBenchmarkClassChecker_InstrumentProvider.builder()
        .optionsModule(OptionsModule.withoutBenchmarkClass(args))
        .outputModule(new OutputModule(new PrintWriter(System.out), new PrintWriter(System.err)))
        .build();

    instruments = instrumentProvider.instruments();
  }

  /**
   * Check to see whether the supplied class contains at least one benchmark method that can be run
   * by caliper.
   *
   * @param theClass the class that may contain one or more benchmark methods.
   * @return true if the class does contain a benchmark method, false otherwise.
   */
  public boolean isBenchmark(Class<?> theClass) {
    for (Method method : theClass.getDeclaredMethods()) {
      for (Instrument instrument : instruments) {
        if (instrument.isBenchmarkMethod(method)) {
          return true;
        }
      }
    }

    return false;
  }

  @Singleton
  @Component(modules = {
      ConfigModule.class,
      ExperimentingRunnerModule.class,
      OptionsModule.class,
      OutputModule.class,
      PlatformModule.class,
      RunnerModule.class,
  })
  /**
   * Provides the set of supported {@link Instrument instruments}.
   */
  interface InstrumentProvider {
    ImmutableSet<Instrument> instruments();
  }
}
