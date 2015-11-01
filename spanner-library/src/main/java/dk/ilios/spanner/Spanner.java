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
import java.util.Arrays;
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

/**
 * Main class for starting a benchmark.
 */
public class Spanner {

    private static final String RUNNER_MAX_PARALLELISM_OPTION = "runner.maxParallelism";

    private final BenchmarkClass benchmarkClass;
    private final Callback callback;
    private SpannerConfig benchmarkConfig;

    public static void runBenchmark(Class benchmarkClass, Method method) throws InvalidBenchmarkException {
        new Spanner(new BenchmarkClass(benchmarkClass, method), null).start();
    }

    public static void runBenchmark(Class benchmarkClass, Method method, Callback callback) throws InvalidBenchmarkException {
        new Spanner(new BenchmarkClass(benchmarkClass, Arrays.asList(method)), callback).start();
    }

    public static void runBenchmarks(Class benchmarkClass, ArrayList<Method> methods) throws InvalidBenchmarkException {
        new Spanner(new BenchmarkClass(benchmarkClass, methods), null).start();
    }

    public static void runBenchmarks(Class benchmarkClass, List<Method> methods, Callback callback) throws InvalidBenchmarkException {
        new Spanner(new BenchmarkClass(benchmarkClass, methods), callback).start();
    }

    public static void runAllBenchmarks(Class benchmarkClass) throws InvalidBenchmarkException {
        new Spanner(new BenchmarkClass(benchmarkClass), null).start();
    }

    public static void runAllBenchmarks(Class benchmarkClass, Callback callback) throws InvalidBenchmarkException {
        new Spanner(new BenchmarkClass(benchmarkClass), callback).start();
    }

    private Spanner(BenchmarkClass benchmarkClass, Callback callback) {
        this.benchmarkClass = benchmarkClass;
        this.callback = callback;
    }

    public void start() {
        try {

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
            OutputFileDumper dumper = new OutputFileDumper(runInfo, benchmarkClass, gson, benchmarkConfig);
            processors.add(dumper);
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

        } catch (InvalidBenchmarkException e) {
            throw new RuntimeException(e);
        } catch (InvalidCommandException e) {
            throw new RuntimeException(e);
        } catch (InvalidConfigurationException e) {
            throw new RuntimeException(e);
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
                throw new InvalidCommandException("Cannot find instrument class '%s'", className);
//                } catch (ProvisionException e) {
//                    throw new InvalidInstrumentException("Could not create the instrument %s", className);
            } catch (InstantiationException e) {
                throw new RuntimeException(e);
            } catch (IllegalAccessException e) {
                throw new RuntimeException(e);
            } catch (InvocationTargetException e) {
                throw new RuntimeException(e);
            }
        }
        return builder.build();
    }

    /**
     * Callback for outside listeners to get notified on the progress of the Benchmarks running.
     */
    public interface Callback {
        void trialStarted(Trial trial);
        void trialSuccess(Trial trial, Trial.Result result);
        void trialFailure(Trial trial, Throwable error);
        void trialEnded(Trial trial);
    }
}
