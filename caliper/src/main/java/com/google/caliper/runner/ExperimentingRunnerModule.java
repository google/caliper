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

package com.google.caliper.runner;

import static com.google.common.base.Preconditions.checkNotNull;

import com.google.caliper.api.ResultProcessor;
import com.google.caliper.config.CaliperConfig;
import com.google.caliper.config.InstrumentConfig;
import com.google.caliper.model.Host;
import com.google.caliper.model.Run;
import com.google.caliper.options.CaliperOptions;
import com.google.caliper.util.InvalidCommandException;
import com.google.caliper.util.Util;
import com.google.common.base.Function;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableSetMultimap;
import com.google.inject.AbstractModule;
import com.google.inject.Injector;
import com.google.inject.Provides;
import com.google.inject.ProvisionException;
import com.google.inject.Singleton;

import org.joda.time.Instant;

import java.io.PrintWriter;
import java.util.UUID;

/**
 * Configures a {@link CaliperRun} that performs experiments.
 */
final class ExperimentingRunnerModule extends AbstractModule {
  private final PrintWriter writer;

  ExperimentingRunnerModule(PrintWriter writer) {
    this.writer = checkNotNull(writer);
  }

  @Override protected void configure() {
    install(new RunnerModule(writer));
    bind(CaliperRun.class).to(ExperimentingCaliperRun.class);
    bind(ExperimentSelector.class).to(FullCartesianExperimentSelector.class);
  }

  @Provides ImmutableSet<ResultProcessor> provideResultProcessors(CaliperConfig config,
      Injector injector) {
    ImmutableSet.Builder<ResultProcessor> builder = ImmutableSet.builder();
    for (Class<? extends ResultProcessor> processorClass : config.getConfiguredResultProcessors()) {
      builder.add(injector.getInstance(processorClass));
    }
    return builder.build();
  }

  @Provides UUID provideUuid() {
    return UUID.randomUUID();
  }

  @Provides @BenchmarkParameters ImmutableSetMultimap<String, String> provideBenchmarkParameters(
      BenchmarkClass benchmarkClass, CaliperOptions options) throws InvalidBenchmarkException {
    return benchmarkClass.userParameters().fillInDefaultsFor(options.userParameters());
  }

  @Provides @Singleton Host provideHost(EnvironmentGetter environmentGetter) {
    return environmentGetter.getHost();
  }

  @Provides @Singleton Run provideRun(UUID id, CaliperOptions options, Instant startTime) {
    return new Run.Builder(id).label(options.runName()).startTime(startTime).build();
  }

  @Provides ImmutableSet<InstrumentConfig> provideInstrumentConfigs(CaliperOptions options,
      final CaliperConfig config) {
    return FluentIterable.from(options.instrumentNames())
        .transform(new Function<String, InstrumentConfig>() {
          @Override public InstrumentConfig apply(String input) {
            return config.getInstrumentConfig(input);
          }
        })
        .toSet();
  }

  @Provides ImmutableSet<Instrument> provideInstrument(Injector injector,
      ImmutableSet<InstrumentConfig> configs) throws InvalidCommandException {
    ImmutableSet.Builder<Instrument> builder = ImmutableSet.builder();
    for (final InstrumentConfig config : configs) {
      String className = config.className();
      Injector instrumentInjector = injector.createChildInjector(new AbstractModule() {
        @Override protected void configure() {
          bind(InstrumentConfig.class).toInstance(config);
        }

        @Provides @InstrumentOptions ImmutableMap<String, String> provideInstrumentOptions(
            InstrumentConfig config) {
          return config.options();
        }
      });
      try {
        builder.add(instrumentInjector.getInstance(
            Util.lenientClassForName(className).asSubclass(Instrument.class)));
      } catch (ClassNotFoundException e) {
        throw new InvalidCommandException("Cannot find instrument class '%s'", className);
      } catch (ProvisionException e) {
        throw new InvalidInstrumentException("Could not create the instrument %s", className);
      }
    }
    return builder.build();
  }

  private static Class<?> classForName(String className)
      throws InvalidCommandException, UserCodeException {
    try {
      return Util.lenientClassForName(className);
    } catch (ClassNotFoundException e) {
      throw new InvalidCommandException("Benchmark class not found: " + className);
    } catch (ExceptionInInitializerError e) {
      throw new UserCodeException(
          "Exception thrown while initializing class '" + className + "'", e.getCause());
    }
  }

  @Provides BenchmarkClass provideBenchmarkClass(CaliperOptions options)
      throws InvalidBenchmarkException, InvalidCommandException {
    BenchmarkClass benchmarkClass = new BenchmarkClass(classForName(options.benchmarkClassName()));
    benchmarkClass.validateParameters(options.userParameters());
    return benchmarkClass;
  }
}
