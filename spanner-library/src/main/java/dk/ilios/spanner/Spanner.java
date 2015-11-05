/*
 * Copyright (C) 2015 Christian Melchior.
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

package dk.ilios.spanner;

import com.google.common.collect.ImmutableSet;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.MoreExecutors;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.internal.bind.TypeAdapters;

import org.threeten.bp.Instant;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.Executors;

import dk.ilios.spanner.config.SpannerConfiguration;
import dk.ilios.spanner.config.SpannerConfigLoader;
import dk.ilios.spanner.config.InstrumentConfig;
import dk.ilios.spanner.config.InvalidConfigurationException;
import dk.ilios.spanner.exception.InvalidCommandException;
import dk.ilios.spanner.http.HttpUploader;
import dk.ilios.spanner.internal.AndroidExperimentSelector;
import dk.ilios.spanner.internal.SpannerRun;
import dk.ilios.spanner.internal.ExperimentSelector;
import dk.ilios.spanner.internal.ExperimentingSpannerRun;
import dk.ilios.spanner.internal.Instrument;
import dk.ilios.spanner.internal.InvalidBenchmarkException;
import dk.ilios.spanner.internal.benchmark.BenchmarkClass;
import dk.ilios.spanner.json.AnnotationExclusionStrategy;
import dk.ilios.spanner.json.InstantTypeAdapter;
import dk.ilios.spanner.log.AndroidStdOut;
import dk.ilios.spanner.log.StdOut;
import dk.ilios.spanner.model.Run;
import dk.ilios.spanner.model.Trial;
import dk.ilios.spanner.options.SpannerOptions;
import dk.ilios.spanner.options.CommandLineOptions;
import dk.ilios.spanner.output.OutputFileDumper;
import dk.ilios.spanner.output.ResultProcessor;
import dk.ilios.spanner.util.NanoTimeGranularityTester;
import dk.ilios.spanner.util.ShortDuration;
import dk.ilios.spanner.util.Util;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Main class for starting a benchmark.
 */
public class Spanner {

    private static final String RUNNER_MAX_PARALLELISM_OPTION = "runner.maxParallelism";

    private final BenchmarkClass benchmarkClass;
    private final Callback callback;
    private SpannerConfig benchmarkConfig;

    public static void runBenchmarks(Class benchmarkClass, ArrayList<Method> methods) {
        runBenchmarks(benchmarkClass, methods, new RethrowCallback());
    }

    public static void runBenchmarks(Class benchmarkClass, List<Method> methods, Callback callback) {
        new Spanner(benchmarkClass, methods, callback).start();
    }

    public static void runAllBenchmarks(Class benchmarkClass) {
        runAllBenchmarks(benchmarkClass, new RethrowCallback());
    }

    public static void runAllBenchmarks(Class benchmarkClass, Callback callback) {
        new Spanner(benchmarkClass, null, callback).start();
    }

    private Spanner(Class benchmarkClass, List<Method> benchmarkMethods, Callback callback) {
        checkNotNull(callback);
        this.callback = callback;
        try {
            this.benchmarkClass = new BenchmarkClass(benchmarkClass, benchmarkMethods);
        } catch (InvalidBenchmarkException e) {
            throw new IllegalArgumentException(e);
        }
    }


    private Spanner(BenchmarkClass benchmarkClass, Callback callback) {
        this.benchmarkClass = benchmarkClass;
        this.callback = callback;
    }

    public void start() {
        try {
            callback.onStart();
            benchmarkConfig = benchmarkClass.getConfiguration();
            File baseline = benchmarkConfig.getBaseLineFile();

            // Setup components needed by the Runner
            SpannerOptions options = CommandLineOptions.parse(new String[]{benchmarkClass.getCanonicalName()});
            SpannerConfigLoader configLoader = new SpannerConfigLoader(options);
            SpannerConfiguration config = configLoader.loadOrCreate();

            ImmutableSet<Instrument> instruments = getInstruments(options, config);

            int poolSize = Integer.parseInt(config.properties().get(RUNNER_MAX_PARALLELISM_OPTION));
            ListeningExecutorService executor = MoreExecutors.listeningDecorator(Executors.newFixedThreadPool(poolSize));

            StdOut stdOut = new AndroidStdOut();
            Run runInfo = new Run.Builder(UUID.randomUUID())
                    .label("Gauge benchmark test")
                    .startTime(Instant.now())
                    .configuration(config)
                    .options(options)
                    .build();

            ExperimentSelector experimentSelector = new AndroidExperimentSelector(benchmarkClass, instruments);

            GsonBuilder gsonBuilder = new GsonBuilder().setExclusionStrategies(new AnnotationExclusionStrategy());
            gsonBuilder.registerTypeAdapterFactory(TypeAdapters.newFactory(Instant.class, new InstantTypeAdapter()));
            Gson gson = gsonBuilder.create();

            Trial[] baselineData;
            if (baseline != null) {
                BufferedReader br = null;
                try {
                    br = new BufferedReader(new FileReader(baseline));
                    baselineData = gson.fromJson(br, Trial[].class);
                    br.close();
                } catch (java.io.IOException e) {
                    throw new RuntimeException(e);
                } finally {
                    if (br != null) {
                        try {
                            br.close();
                        } catch (IOException ignored) {
                        }
                    }
                }
            } else {
                baselineData = new Trial[0];
            }

            Set<ResultProcessor> processors = new HashSet<>();
            if (benchmarkConfig.getResultsFolder() != null) {
                OutputFileDumper dumper = new OutputFileDumper(runInfo, benchmarkClass, gson, benchmarkConfig.getResultsFolder());
                processors.add(dumper);
            }
            if (benchmarkConfig.getBaselineOutputFile() != null) {
                OutputFileDumper dumper = new OutputFileDumper(runInfo, benchmarkClass, gson, benchmarkConfig.getBaselineOutputFile());
                processors.add(dumper);
            }
            if (benchmarkConfig.isUploadResults()) {
                HttpUploader uploader = new HttpUploader(stdOut, gson, benchmarkConfig);
                processors.add(uploader);
            }

            ImmutableSet<ResultProcessor> resultProcessors = ImmutableSet.copyOf(processors);

            // Configure runner
            SpannerRun run = new ExperimentingSpannerRun(
                    options,
                    stdOut,
                    runInfo,
                    instruments,
                    resultProcessors,
                    experimentSelector,
                    executor,
                    baselineData,
                    callback
            );

            // Run benchmark
            run.run();
            callback.onComplete();
        } catch (InvalidBenchmarkException e) {
            callback.onError(e);
        } catch (InvalidCommandException e) {
            callback.onError(e);
        } catch (InvalidConfigurationException e) {
            callback.onError(e);
        }
    }

    public ImmutableSet<Instrument> getInstruments(SpannerOptions options, final SpannerConfiguration config) throws InvalidCommandException {
        ImmutableSet.Builder<Instrument> builder = ImmutableSet.builder();
        ImmutableSet<String> configuredInstruments = config.getConfiguredInstruments();
        for (final String instrumentName : options.instrumentNames()) {
            if (!configuredInstruments.contains(instrumentName)) {
                throw new InvalidCommandException("%s is not a configured instrument (%s). "
                        + "use --print-config to see the configured instruments.",
                        instrumentName, configuredInstruments);
            }
            final InstrumentConfig instrumentConfig = config.getInstrumentConfig(instrumentName);
//                Injector instrumentInjector = injector.createChildInjector(new AbstractModule() {
//                    @Override protected void configure() {
//                        bind(InstrumentConfig.class).toInstance(instrumentConfig);
//                    }
//
//                    @Provides @InstrumentOptions
//                    ImmutableMap<String, String> provideInstrumentOptions(InstrumentConfig config) {
//                        return config.options();
//                    }
//
//                    @Provides @InstrumentName String provideInstrumentName() {
//                        return instrumentName;
//                    }
//                });
            String className = instrumentConfig.className();
            try {
                Class<? extends Instrument> clazz = Util.lenientClassForName(className).asSubclass(Instrument.class);
                ShortDuration timerGranularity = new NanoTimeGranularityTester().testNanoTimeGranularity();
                Instrument instrument = (Instrument) clazz.getDeclaredConstructors()[0].newInstance(timerGranularity);
                instrument.setOptions(config.properties());
                builder.add(instrument);
            } catch (ClassNotFoundException e) {
                callback.onError(new InvalidCommandException("Cannot find instrument class '%s'", className));
            } catch (InstantiationException e) {
                callback.onError(e);
            } catch (IllegalAccessException e) {
                callback.onError(e);
            } catch (InvocationTargetException e) {
                callback.onError(e);
            }
        }
        return builder.build();
    }

    /**
     * Callback for outside listeners to get notified on the progress of the Benchmarks running.
     */
    public interface Callback {
        void onStart();
        void trialStarted(Trial trial);
        void trialSuccess(Trial trial, Trial.Result result);
        void trialFailure(Trial trial, Throwable error);
        void trialEnded(Trial trial);
        void onComplete();
        void onError(Exception exception);
    }

    private static class RethrowCallback extends SpannerCallbackAdapter {
        @Override
        public void onError(Exception error) {
            throw new RuntimeException(error);
        }
    }
}
