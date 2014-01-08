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

import com.google.caliper.api.ResultProcessor;
import com.google.caliper.config.CaliperConfig;
import com.google.caliper.config.InstrumentConfig;
import com.google.caliper.model.Host;
import com.google.caliper.model.Run;
import com.google.caliper.options.CaliperOptions;
import com.google.caliper.runner.Instrument.Instrumentation;
import com.google.caliper.util.InvalidCommandException;
import com.google.caliper.util.ShortDuration;
import com.google.caliper.util.Util;
import com.google.common.base.Function;
import com.google.common.collect.HashMultiset;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableSetMultimap;
import com.google.common.collect.ImmutableSortedSet;
import com.google.common.collect.Multiset;
import com.google.common.collect.Multisets;
import com.google.common.collect.Ordering;
import com.google.common.collect.TreeMultiset;
import com.google.inject.AbstractModule;
import com.google.inject.Injector;
import com.google.inject.Provides;
import com.google.inject.ProvisionException;
import com.google.inject.Singleton;
import com.google.inject.assistedinject.FactoryModuleBuilder;

import org.joda.time.Instant;

import java.lang.reflect.Method;
import java.util.UUID;

/**
 * Configures a {@link CaliperRun} that performs experiments.
 */
final class ExperimentingRunnerModule extends AbstractModule {
  @Override protected void configure() {
    install(new RunnerModule());
    bind(CaliperRun.class).to(ExperimentingCaliperRun.class);
    bind(ExperimentSelector.class).to(FullCartesianExperimentSelector.class);
    install(new FactoryModuleBuilder()
        .implement(StreamService.class, StreamService.class)
        .build(StreamService.Factory.class));
    install(new FactoryModuleBuilder()
        .implement(WorkerProcess.class, WorkerProcess.class)
        .build(WorkerProcess.Factory.class));
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

  @Provides ImmutableSet<Instrument> provideInstruments(Injector injector,
      CaliperOptions options, final CaliperConfig config) throws InvalidCommandException {
    ImmutableSet.Builder<Instrument> builder = ImmutableSet.builder();
    ImmutableSet<String> configuredInstruments = config.getConfiguredInstruments();
    for (final String instrumentName : options.instrumentNames()) {
      if (!configuredInstruments.contains(instrumentName)) {
        throw new InvalidCommandException("%s is not a configured instrument (%s). "
            + "use --print-config to see the configured instruments.",
                instrumentName, configuredInstruments);
      }
      final InstrumentConfig instrumentConfig = config.getInstrumentConfig(instrumentName);
      Injector instrumentInjector = injector.createChildInjector(new AbstractModule() {
        @Override protected void configure() {
          bind(InstrumentConfig.class).toInstance(instrumentConfig);
        }

        @Provides @InstrumentOptions ImmutableMap<String, String> provideInstrumentOptions(
            InstrumentConfig config) {
          return config.options();
        }

        @Provides @InstrumentName String provideInstrumentName() {
          return instrumentName;
        }
      });
      String className = instrumentConfig.className();
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

  @Provides @Singleton @NanoTimeGranularity ShortDuration provideNanoTimeGranularity(
      NanoTimeGranularityTester tester) {
    return tester.testNanoTimeGranularity();
  }

  @Provides ImmutableSet<Instrumentation> provideInstrumentations(CaliperOptions options,
      BenchmarkClass benchmarkClass, ImmutableSet<Instrument> instruments)
          throws InvalidBenchmarkException {
    ImmutableSet.Builder<Instrumentation> builder = ImmutableSet.builder();
    ImmutableSet<String> benchmarkMethodNames = options.benchmarkMethodNames();
    for (Instrument instrument : instruments) {
      for (Method method : findAllBenchmarkMethods(benchmarkClass.benchmarkClass(), instrument)) {
        if (benchmarkMethodNames.isEmpty() || benchmarkMethodNames.contains(method.getName())) {
          builder.add(instrument.createInstrumentation(method));
        }
      }
    }
    return builder.build();
  }

  private static ImmutableSortedSet<Method> findAllBenchmarkMethods(Class<?> benchmarkClass,
      Instrument instrument) throws InvalidBenchmarkException {
    ImmutableSortedSet.Builder<Method> result = ImmutableSortedSet.orderedBy(
        Ordering.natural().onResultOf(new Function<Method, String>() {
          @Override public String apply(Method method) {
            return method.getName();
          }
        }));
    // use a TreeMultiset so iteration order is consistent no matter what order methods are added.
    Multiset<String> benchmarkMethodNames = TreeMultiset.create();
    for (Method method : benchmarkClass.getDeclaredMethods()) {
      if (instrument.isBenchmarkMethod(method)) {
        method.setAccessible(true);
        result.add(method);
        benchmarkMethodNames.add(method.getName());
      }
    }
    // remove all benchmark methods with unique names (i.e. counts of 1)
    Multisets.removeOccurrences(benchmarkMethodNames,
        HashMultiset.create(benchmarkMethodNames.elementSet()));
    if (!benchmarkMethodNames.isEmpty()) {
      throw new InvalidBenchmarkException(
          "Overloads are disallowed for benchmark methods, found overloads of %s in benchmark %s",
          benchmarkMethodNames.elementSet(),
          benchmarkClass);
    }
    return result.build();
  }
}
