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
import com.google.caliper.options.CaliperOptions;
import com.google.caliper.platform.Platform;
import com.google.caliper.runner.Instrument.Instrumentation;
import com.google.caliper.util.InvalidCommandException;
import com.google.caliper.util.ShortDuration;
import com.google.caliper.util.Stderr;
import com.google.caliper.util.Util;
import com.google.common.base.Function;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableSetMultimap;
import com.google.common.collect.ImmutableSortedSet;
import com.google.common.collect.Ordering;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.MoreExecutors;
import com.google.common.util.concurrent.Service;

import dagger.MapKey;
import dagger.Module;
import dagger.Provides;
import dagger.multibindings.IntoMap;
import dagger.multibindings.IntoSet;

import java.io.PrintWriter;
import java.lang.reflect.Method;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.UUID;
import java.util.concurrent.Executors;

import javax.inject.Provider;
import javax.inject.Singleton;

/**
 * Configures a {@link CaliperRun} that performs experiments.
 */
@Module
final class ExperimentingRunnerModule {
  private static final String RUNNER_MAX_PARALLELISM_OPTION = "runner.maxParallelism";

  @Provides
  @IntoSet
  static Service provideServerSocketService(ServerSocketService impl) {
    return impl;
  }

  @Provides
  @IntoSet
  static Service provideTrialOutputFactoryService(TrialOutputFactoryService impl) {
    return impl;
  }

  @Provides
  static TrialOutputFactory provideTrialOutputFactory(TrialOutputFactoryService impl) {
    return impl;
  }

  @Provides
  static ExperimentSelector provideExperimentSelector(FullCartesianExperimentSelector impl) {
    return impl;
  }

  @Provides
  static ListeningExecutorService provideExecutorService(CaliperConfig config) {
    int poolSize = Integer.parseInt(config.properties().get(RUNNER_MAX_PARALLELISM_OPTION));
    return MoreExecutors.listeningDecorator(Executors.newFixedThreadPool(poolSize));
  }

  @LocalPort
  @Provides
  static int providePortNumber(ServerSocketService serverSocketService) {
    return serverSocketService.getPort();
  }

  /**
   * Specifies the {@link Class} object to use as a key in the map of available
   * {@link ResultProcessor result processors} passed to
   * {@link #provideResultProcessors(CaliperConfig, Map)}.
   */
  @MapKey(unwrapValue = true)
  public @interface ResultProcessorClassKey {
    Class<? extends ResultProcessor> value();
  }

  @Provides
  @IntoMap
  @ResultProcessorClassKey(OutputFileDumper.class)
  static ResultProcessor provideOutputFileDumper(OutputFileDumper impl) {
    return impl;
  }

  @Provides
  @IntoMap
  @ResultProcessorClassKey(HttpUploader.class)
  static ResultProcessor provideHttpUploader(HttpUploader impl) {
    return impl;
  }

  @Provides static ImmutableSet<ResultProcessor> provideResultProcessors(
      CaliperConfig config,
      Map<Class<? extends ResultProcessor>, Provider<ResultProcessor>> availableProcessors) {
    ImmutableSet.Builder<ResultProcessor> builder = ImmutableSet.builder();
    for (Class<? extends ResultProcessor> processorClass : config.getConfiguredResultProcessors()) {
      Provider<ResultProcessor> resultProcessorProvider = availableProcessors.get(processorClass);
      ResultProcessor resultProcessor = resultProcessorProvider == null
          ? ResultProcessorCreator.createResultProcessor(processorClass)
          : resultProcessorProvider.get();
      builder.add(resultProcessor);
    }
    return builder.build();
  }

  @Provides static UUID provideUuid() {
    return UUID.randomUUID();
  }

  @Provides @BenchmarkParameters
  static ImmutableSetMultimap<String, String> provideBenchmarkParameters(
      BenchmarkClass benchmarkClass, CaliperOptions options) throws InvalidBenchmarkException {
    return benchmarkClass.userParameters().fillInDefaultsFor(options.userParameters());
  }

  @Provides @Singleton
  static Host provideHost(EnvironmentGetter environmentGetter) {
    return environmentGetter.getHost();
  }

  @Provides @Singleton
  static EnvironmentGetter provideEnvironmentGetter() {
    return new EnvironmentGetter();
  }

  /**
   * Specifies the {@link Class} object to use as a key in the map of available
   * {@link Instrument instruments} passed to {@link #provideInstruments},
   */
  @MapKey(unwrapValue = true)
  public @interface InstrumentClassKey {
    Class<? extends Instrument> value();
  }

  @Provides
  @IntoMap
  @InstrumentClassKey(ArbitraryMeasurementInstrument.class)
  static Instrument provideArbitraryMeasurementInstrument() {
    return new ArbitraryMeasurementInstrument();
  }

  @Provides
  @IntoMap
  @InstrumentClassKey(AllocationInstrument.class)
  static Instrument provideAllocationInstrument() {
    return new AllocationInstrument();
  }

  @Provides
  @IntoMap
  @InstrumentClassKey(RuntimeInstrument.class)
  static Instrument provideRuntimeInstrument(
      @NanoTimeGranularity ShortDuration nanoTimeGranularity) {
    return new RuntimeInstrument(nanoTimeGranularity);
  }

  @Provides
  static ImmutableSet<Instrument> provideInstruments(
      CaliperOptions options,
      final CaliperConfig config,
      Map<Class<? extends Instrument>, Provider<Instrument>> availableInstruments,
      Platform platform,
      @Stderr PrintWriter stderr)
      throws InvalidCommandException {

    ImmutableSet.Builder<Instrument> builder = ImmutableSet.builder();
    ImmutableSet<String> configuredInstruments = config.getConfiguredInstruments();
    for (final String instrumentName : options.instrumentNames()) {
      if (!configuredInstruments.contains(instrumentName)) {
        throw new InvalidCommandException("%s is not a configured instrument (%s). "
            + "use --print-config to see the configured instruments.",
            instrumentName, configuredInstruments);
      }
      final InstrumentConfig instrumentConfig = config.getInstrumentConfig(instrumentName);
      String className = instrumentConfig.className();
      try {
        Class<? extends Instrument> clazz =
            Util.lenientClassForName(className).asSubclass(Instrument.class);
        Provider<Instrument> instrumentProvider = availableInstruments.get(clazz);
        if (instrumentProvider == null) {
          throw new InvalidInstrumentException("Instrument %s not supported", className);
        }

        // Make sure that the instrument is supported on the platform.
        if (platform.supports(clazz)) {
          Instrument instrument = instrumentProvider.get();
          InstrumentInjectorModule injectorModule =
              new InstrumentInjectorModule(instrumentConfig, instrumentName);
          InstrumentComponent instrumentComponent = DaggerInstrumentComponent.builder()
              .instrumentInjectorModule(injectorModule)
              .build();
          instrumentComponent.injectInstrument(instrument);
          builder.add(instrument);
        } else {
          stderr.format("Instrument %s not supported on %s, ignoring\n",
              className, platform.name());
        }
      } catch (ClassNotFoundException e) {
        throw new InvalidCommandException("Cannot find instrument class '%s'", className);
      }
    }
    return builder.build();
  }

  @Provides @Singleton static NanoTimeGranularityTester provideNanoTimeGranularityTester() {
    return new NanoTimeGranularityTester();
  }

  @Provides @Singleton @NanoTimeGranularity static ShortDuration provideNanoTimeGranularity(
      NanoTimeGranularityTester tester) {
    return tester.testNanoTimeGranularity();
  }

  @Provides static ImmutableSet<Instrumentation> provideInstrumentations(CaliperOptions options,
      BenchmarkClass benchmarkClass, ImmutableSet<Instrument> instruments)
          throws InvalidBenchmarkException {
    ImmutableSet.Builder<Instrumentation> builder = ImmutableSet.builder();
    ImmutableSet<String> benchmarkMethodNames = options.benchmarkMethodNames();
    Set<String> unusedBenchmarkNames = new HashSet<String>(benchmarkMethodNames);
    for (Instrument instrument : instruments) {
      for (Method method : findAllBenchmarkMethods(benchmarkClass.benchmarkClass(), instrument)) {
        if (benchmarkMethodNames.isEmpty() || benchmarkMethodNames.contains(method.getName())) {
          builder.add(instrument.createInstrumentation(method));
          unusedBenchmarkNames.remove(method.getName());
        }
      }
    }
    if (!unusedBenchmarkNames.isEmpty()) {
      throw new InvalidBenchmarkException(
          "Invalid benchmark method(s) specified in options: " + unusedBenchmarkNames);
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
    Set<String> benchmarkMethodNames = new HashSet<String>();
    Set<String> overloadedMethodNames = new TreeSet<String>();
    for (Method method : benchmarkClass.getDeclaredMethods()) {
      if (instrument.isBenchmarkMethod(method)) {
        method.setAccessible(true);
        result.add(method);
        if (!benchmarkMethodNames.add(method.getName())) {
          overloadedMethodNames.add(method.getName());
        }
      }
    }
    if (!overloadedMethodNames.isEmpty()) {
      throw new InvalidBenchmarkException(
          "Overloads are disallowed for benchmark methods, found overloads of %s in benchmark %s",
          overloadedMethodNames,
          benchmarkClass);
    }
    return result.build();
  }
}
