/*
 * Copyright (C) 2011 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */

package com.google.caliper.runner;

import static com.google.common.base.Charsets.UTF_8;
import static com.google.common.base.Preconditions.checkState;
import static java.util.concurrent.TimeUnit.NANOSECONDS;
import static java.util.logging.Level.SEVERE;
import static java.util.logging.Level.WARNING;

import com.google.caliper.Benchmark;
import com.google.caliper.api.ResultProcessor;
import com.google.caliper.api.SkipThisScenarioException;
import com.google.caliper.bridge.AbstractLogMessageVisitor;
import com.google.caliper.bridge.CaliperControlLogMessage;
import com.google.caliper.bridge.FailureLogMessage;
import com.google.caliper.bridge.LogMessage;
import com.google.caliper.bridge.LogMessageVisitor;
import com.google.caliper.bridge.VmOptionLogMessage;
import com.google.caliper.bridge.VmPropertiesLogMessage;
import com.google.caliper.bridge.WorkerSpec;
import com.google.caliper.config.CaliperConfig;
import com.google.caliper.model.BenchmarkSpec;
import com.google.caliper.model.Host;
import com.google.caliper.model.Run;
import com.google.caliper.model.Scenario;
import com.google.caliper.model.Trial;
import com.google.caliper.model.VmSpec;
import com.google.caliper.options.CaliperOptions;
import com.google.caliper.runner.Instrument.MeasurementCollectingVisitor;
import com.google.caliper.util.Parser;
import com.google.caliper.util.Pipes;
import com.google.caliper.util.ShortDuration;
import com.google.caliper.util.Stderr;
import com.google.caliper.util.Stdout;
import com.google.caliper.worker.WorkerMain;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Function;
import com.google.common.base.Optional;
import com.google.common.base.Predicate;
import com.google.common.base.Stopwatch;
import com.google.common.base.Throwables;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableSortedMap;
import com.google.common.collect.Iterables;
import com.google.common.collect.Maps;
import com.google.common.collect.Queues;
import com.google.common.io.Closeables;
import com.google.common.io.LineReader;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.MoreExecutors;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.google.gson.Gson;
import com.google.inject.Inject;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.Reader;
import java.nio.charset.Charset;
import java.text.ParseException;
import java.util.Collections;
import java.util.List;
import java.util.Map.Entry;
import java.util.UUID;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.logging.Logger;

import javax.annotation.Nullable;

/**
 * An execution of each {@link Experiment} for the configured number of trials.
 */
@VisibleForTesting
public final class ExperimentingCaliperRun implements CaliperRun {
  private static final Logger logger = Logger.getLogger(ExperimentingCaliperRun.class.getName());

  private final CaliperOptions options;
  private final CaliperConfig config;
  private final PrintWriter stdout;
  private final PrintWriter stderr;
  private final BenchmarkClass benchmarkClass;
  private final ImmutableSet<Instrument> instruments;
  private final ImmutableSet<ResultProcessor> resultProcessors;
  private final Parser<LogMessage> logMessageParser;
  private final ExperimentSelector selector;
  private final Host host;
  private final Run run;
  private final Gson gson;
  private final ListeningExecutorService consumerExecutor =
      MoreExecutors.listeningDecorator(Executors.newSingleThreadExecutor(
          new ThreadFactoryBuilder()
              .setNameFormat("line-processor-%d")
              .setDaemon(true)
              .build()));
  private final ListeningExecutorService processExecutor =
      MoreExecutors.listeningDecorator(Executors.newSingleThreadExecutor(
          new ThreadFactoryBuilder()
              .setNameFormat("process-watcher-%d")
              .setDaemon(true)
              .build()));

  private final Stopwatch trialStopwatch = Stopwatch.createUnstarted();
  /** This is 1-indexed because it's only used for display to users.  E.g. "Trial 1 of 27" */
  private volatile int trialNumber = 1;

  @Inject @VisibleForTesting
  public ExperimentingCaliperRun(
      CaliperOptions options,
      CaliperConfig config,
      @Stdout PrintWriter stdout,
      @Stderr PrintWriter stderr,
      BenchmarkClass benchmarkClass,
      ImmutableSet<Instrument> instruments,
      ImmutableSet<ResultProcessor> resultProcessors,
      Parser<LogMessage> logMessageParser,
      ExperimentSelector selector,
      Host host,
      Run run,
      Gson gson) {
    this.options = options;
    this.config = config;
    this.stdout = stdout;
    this.stderr = stderr;
    this.benchmarkClass = benchmarkClass;
    this.instruments = instruments;
    this.resultProcessors = resultProcessors;
    this.logMessageParser = logMessageParser;
    this.selector = selector;
    this.host = host;
    this.run = run;
    this.gson = gson;
  }

  @Override
  public void run() throws InvalidBenchmarkException {
    // TODO(gak): this class is getting big again.  is there a better place for this?
    if (options.printConfiguration()) {
      stdout.println("Configuration:");
      ImmutableSortedMap<String, String> sortedProperties =
          ImmutableSortedMap.copyOf(config.properties());
      for (Entry<String, String> entry : sortedProperties.entrySet()) {
        stdout.printf("  %s = %s%n", entry.getKey(), entry.getValue());
      }
    }

    stdout.println("Experiment selection: ");
    stdout.println("  Instruments:   " + FluentIterable.from(selector.instruments())
        .transform(new Function<Instrument, String>() {
              @Override public String apply(Instrument instrument) {
                return instrument.name();
              }
            }));
    stdout.println("  User parameters:   " + selector.userParameters());
    stdout.println("  Virtual machines:  " + FluentIterable.from(selector.vms())
        .transform(
            new Function<VirtualMachine, String>() {
              @Override public String apply(VirtualMachine vm) {
                return vm.name;
              }
            }));
    stdout.println("  Selection type:    " + selector.selectionType());
    stdout.println();

    ImmutableSet<Experiment> allExperiments = selector.selectExperiments();
    if (allExperiments.isEmpty()) {
      throw new InvalidBenchmarkException(
          "There were no experiments to be peformed for the class %s using the instruments %s",
          benchmarkClass.benchmarkClass().getSimpleName(), instruments);
    }

    stdout.format("This selection yields %s experiments.%n", allExperiments.size());
    stdout.flush();

    // always dry run first.
    ImmutableSet<Experiment> experimentsToRun = dryRun(allExperiments);
    if (experimentsToRun.size() != allExperiments.size()) {
      stdout.format("%d experiments were skipped.%n",
          allExperiments.size() - experimentsToRun.size());
    }

    if (experimentsToRun.isEmpty()) {
      throw new InvalidBenchmarkException("All experiements were skipped.");
    }

    if (options.dryRun()) {
      return;
    }

    stdout.flush();

    int totalTrials = experimentsToRun.size() * options.trialsPerScenario();
    Stopwatch stopwatch = Stopwatch.createStarted();

    try {
      for (int i = 0; i < options.trialsPerScenario(); i++) {
        for (Experiment experiment : experimentsToRun) {
          stdout.printf("Starting experiment %d of %d: %s%n", trialNumber, totalTrials, experiment);
          try {
            Trial trial = measure(experiment);
            stdout.println("Complete!");
            for (ResultProcessor resultProcessor : resultProcessors) {
              resultProcessor.processTrial(trial);
            }
          } catch (TrialFailureException e) {
            stderr.println(
                "ERROR: Trial failed to complete (its results will not be included in the run):\n"
                    + "  " + e.getMessage());
          } catch (IOException e) {
            throw Throwables.propagate(e);
          } finally {
            trialNumber++;
          }
        }
      }
    } finally {
      consumerExecutor.shutdown();
    }

    stdout.print("\n");
    stdout.format("Execution complete: %s.%n",
        ShortDuration.of(stopwatch.stop().elapsed(NANOSECONDS), NANOSECONDS));

    for (ResultProcessor resultProcessor : resultProcessors) {
      try {
        resultProcessor.close();
      } catch (IOException e) {
        logger.log(WARNING, "Could not close a result processor: " + resultProcessor, e);
      }
    }
  }

  private static final int NUM_WORKER_STREAMS = 3;

  private ProcessBuilder createWorkerProcessBuilder(Experiment experiment,
      BenchmarkSpec benchmarkSpec, File pipeFile) {
    Instrument instrument = experiment.instrument();

    WorkerSpec request = new WorkerSpec(instrument.workerClass().getName(),
        instrument.workerOptions(), benchmarkSpec, pipeFile.getAbsolutePath());

    ProcessBuilder processBuilder = new ProcessBuilder().redirectErrorStream(false);

    List<String> args = processBuilder.command();

    String jvmName = experiment.vm().name;

    String jdkPath = experiment.vm().config.javaExecutable().getAbsolutePath();
    args.add(jdkPath);
    logger.fine(String.format("Java(%s) Path: %s", jvmName, jdkPath));

    ImmutableList<String> jvmOptions = experiment.vm().config.options();
    args.addAll(jvmOptions);
    logger.fine(String.format("Java(%s) args: %s", jvmName, jvmOptions));

    ImmutableSet<String> benchmarkJvmOptions = benchmarkClass.vmOptions();
    args.addAll(benchmarkJvmOptions);
    logger.fine(String.format("Benchmark(%s) Java args: %s", benchmarkClass.name(),
        benchmarkJvmOptions));

    String classPath = System.getProperty("java.class.path");
    Collections.addAll(args, "-cp", classPath);
    logger.finer(String.format("Class path: %s", classPath));

    Iterable<String> instrumentJvmOptions = instrument.getExtraCommandLineArgs();
    Iterables.addAll(args, instrumentJvmOptions);
    logger.fine(String.format("Instrument(%s) Java args: %s", instrument.getClass().getName(),
        instrumentJvmOptions));

    // last to ensure that they're always applied
    args.add("-XX:+PrintFlagsFinal");
    args.add("-XX:+PrintCompilation");
    args.add("-XX:+PrintGC");

    args.add(WorkerMain.class.getName());
    args.add(gson.toJson(request));

    logger.finest(String.format("Full JVM (%s) args: %s", jvmName, args));
    return processBuilder;
  }

  private Trial measure(Experiment experiment) throws IOException {
    BenchmarkSpec benchmarkSpec = new BenchmarkSpec.Builder()
        .className(experiment.benchmarkMethod().benchmarkClass().name())
        .methodName(experiment.benchmarkMethod().name())
        .addAllParameters(experiment.userParameters())
        .build();

    final File pipeFile = Pipes.createPipe();

    final WorkerProcess process =
        new WorkerProcess(createWorkerProcessBuilder(experiment, benchmarkSpec, pipeFile));
    ListenableFuture<Integer> processFuture = processExecutor.submit(new Callable<Integer>() {
      @Override public Integer call() throws Exception {
        return process.waitFor();
      }
    });

    trialStopwatch.start();

    final ListeningExecutorService producerExecutor = MoreExecutors.listeningDecorator(
        Executors.newFixedThreadPool(NUM_WORKER_STREAMS, new ThreadFactoryBuilder()
            .setDaemon(true)
            .setNameFormat("stream-listener-%d")
            .build()));
    try {
      final BlockingQueue<String> queue = Queues.newLinkedBlockingQueue();

      // use the default charset because worker streams will use the default for output
      Charset processCharset = Charset.defaultCharset();
      ListenableFuture<Void> inputFuture = producerExecutor.submit(
          new LineProducer(new InputStreamReader(process.getInputStream(), processCharset), queue));
      ListenableFuture<Void> errorFuture = producerExecutor.submit(
          new LineProducer(new InputStreamReader(process.getErrorStream(), processCharset), queue));
      final ListenableFuture<Reader> pipeReaderFuture =
          producerExecutor.submit(new Callable<Reader>() {
        @Override public Reader call() throws IOException {
          return new InputStreamReader(new FileInputStream(pipeFile), UTF_8);
        }
      });
      processFuture.addListener(new Runnable() {
        @Override public void run() {
          if (!pipeReaderFuture.isDone()) {
            // the process completed without the pipe ever being written to.  it crashed.
            // TODO(gak): get the output from the worker so we can know why it crashed
            stdout.print("The worker exited without producing data. "
                + "It has likely crashed. Run with --verbose to see any worker output.\n");
            stdout.flush();
            System.exit(1);
          }
        }
      }, MoreExecutors.sameThreadExecutor());
      Futures.addCallback(pipeReaderFuture, new FutureCallback<Reader>() {
        @Override public void onSuccess(Reader result) {
          logger.fine("successfully opened the pipe from the worker");
          producerExecutor.submit(new LineProducer(result, queue));
        }

        @Override public void onFailure(Throwable t) {
          logger.log(SEVERE, "Could not open the pipe from the worker", t);
          // don't worry about propagating the exception since the future will take care of it
        }
      });

      MeasurementCollectingVisitor measurementCollectingVisitor =
          experiment.instrument().getMeasurementCollectingVisitor();
      DataCollectingVisitor dataCollectingVisitor = new DataCollectingVisitor();

      /*
       * Start watching the queue before we wait on the pipe so that we can see output from failed
       * workers.
       */
      ListenableFuture<Void> consumerFuture = consumerExecutor.submit(
          new LineConsumer(queue, measurementCollectingVisitor,
              ImmutableSet.of(dataCollectingVisitor)));
      consumerFuture.addListener(new Runnable() {
        @Override public void run() {
          // kill the process because we're all done
          // TODO(gak): send SIGINT instead of SIGTERM for clean exit
          process.destroy();
        }
      }, MoreExecutors.sameThreadExecutor());

      process.waitFor();
      consumerFuture.get();

      ImmutableMap<String, String> vmOptions = dataCollectingVisitor.vmOptionsBuilder.build();
      checkState(!vmOptions.isEmpty());
      VmSpec vmSpec = new VmSpec.Builder()
          .addAllProperties(dataCollectingVisitor.vmProperties.get())
          .addAllOptions(vmOptions)
          .build();
      return new Trial.Builder(UUID.randomUUID())
          .run(run)
          .instrumentSpec(experiment.instrument().getSpec())
          .scenario(new Scenario.Builder()
              .host(host)
              .vmSpec(vmSpec)
              .benchmarkSpec(benchmarkSpec))
          .addAllMeasurements(measurementCollectingVisitor.getMeasurements())
          .build();
    } catch (InterruptedException e) {
      throw new AssertionError();
    } catch (ExecutionException e) {
      Throwable cause = e.getCause();
      Throwables.propagateIfInstanceOf(cause, TrialFailureException.class);
      throw new RuntimeException(cause);
    } finally {
      trialStopwatch.reset();
      producerExecutor.shutdownNow();
    }
  }

  private long getRemainingTrialNanos() {
    ShortDuration timeLimit = options.timeLimit();
    if (ShortDuration.zero().equals(timeLimit)) {
      return Long.MAX_VALUE;
    }
    return timeLimit.to(NANOSECONDS) - trialStopwatch.elapsed(NANOSECONDS);
  }

  /**
   * Attempts to run each given scenario once, in the current VM. Returns a set of all of the
   * scenarios that didn't throw a {@link SkipThisScenarioException}.
   */
  ImmutableSet<Experiment> dryRun(Iterable<Experiment> experiments)
      throws InvalidBenchmarkException {
    ImmutableSet.Builder<Experiment> builder = ImmutableSet.builder();
    for (Experiment experiment : experiments) {
      try {
        Benchmark benchmark = benchmarkClass.createAndStage(experiment.userParameters());
        try {
          experiment.instrument().dryRun(benchmark, experiment.benchmarkMethod());
          builder.add(experiment);
        } finally {
          // discard 'benchmark' now; the worker will have to instantiate its own anyway
          benchmarkClass.cleanup(benchmark);
        }
      } catch (SkipThisScenarioException innocuous) {}
    }
    return builder.build();
  }

  private static final class DataCollectingVisitor extends AbstractLogMessageVisitor {
    final ImmutableMap.Builder<String, String> vmOptionsBuilder = ImmutableMap.builder();
    Optional<ImmutableMap<String, String>> vmProperties = Optional.absent();

    @Override
    public void visit(FailureLogMessage logMessage) {
      throw new ProxyWorkerException(logMessage.exceptionClassName(), logMessage.message(),
          logMessage.stackTrace());
    }

    @Override
    public void visit(VmOptionLogMessage logMessage) {
      vmOptionsBuilder.put(logMessage.name(), logMessage.value());
    }

    static final Predicate<String> PROPERTIES_TO_RETAIN = new Predicate<String>() {
      @Override public boolean apply(String input) {
        return input.startsWith("java.vm")
            || input.startsWith("java.runtime")
            || input.equals("java.version")
            || input.equals("java.vendor")
            || input.equals("sun.reflect.noInflation")
            || input.equals("sun.reflect.inflationThreshold");
      }
    };

    @Override
    public void visit(VmPropertiesLogMessage logMessage) {
      vmProperties = Optional.of(ImmutableMap.copyOf(
          Maps.filterKeys(logMessage.properties(), PROPERTIES_TO_RETAIN)));
    }
  }

  // a new instance to be extra careful about using ==
  private static final String POISON_PILL = new String("Bel Biv Devoe");

  private static final class LineProducer implements Callable<Void> {
    final Reader reader;
    final BlockingQueue<String> queue;


    LineProducer(Reader reader, BlockingQueue<String> queue) {
      this.reader = reader;
      this.queue = queue;
    }

    @Override
    public Void call() throws IOException, InterruptedException {
      LineReader lineReader = new LineReader(reader);
      boolean threw = true;
      try {
        String line;
        while ((line = lineReader.readLine()) != null) {
          queue.put(line);
        }
        threw = false;
      } finally {
        queue.put(POISON_PILL);
        Closeables.close(reader, threw);
      }
      return null;
    }
  }

  private final class LineConsumer implements Callable<Void> {
    final BlockingQueue<String> queue;
    final MeasurementCollectingVisitor measurementCollectingVisitor;
    final ImmutableSet<? extends LogMessageVisitor> otherVisitors;

    LineConsumer(BlockingQueue<String> queue,
        MeasurementCollectingVisitor measurementCollectingVisitor,
        ImmutableSet<? extends LogMessageVisitor> otherVisitors) {
      this.queue = queue;
      this.measurementCollectingVisitor = measurementCollectingVisitor;
      this.otherVisitors = otherVisitors;
    }

    @Override public Void call() throws InterruptedException {
      int poisonPillsSeen = 0;

      while ((poisonPillsSeen < NUM_WORKER_STREAMS)
          && !measurementCollectingVisitor.isDoneCollecting()) {
        @Nullable String line = queue.poll(getRemainingTrialNanos(), NANOSECONDS);
        if (line == null) {
          // timed out before we got through the queue
          throw new TrialFailureException(String.format(
              "Trial exceeded the total allowable runtime (%s). "
                  + "The limit may be adjusted using the --time-limit flag.",
                      options.timeLimit()));
        } else if (line == POISON_PILL) {
          poisonPillsSeen++;
        } else {
          processLine(line);
        }
      }

      trialStopwatch.stop();
      logger.fine("trial completed in " + trialStopwatch);

      return null;
    }

    void processLine(String line) {
      try {
        LogMessage logMessage = logMessageParser.parse(line);
        if (options.verbose() && !(logMessage instanceof CaliperControlLogMessage)) {
          stdout.printf("[trial-%d] %s%n", trialNumber, line);
        }
        logMessage.accept(measurementCollectingVisitor);
        for (LogMessageVisitor visitor : otherVisitors) {
          logMessage.accept(visitor);
        }
      } catch (ParseException e) {
        throw new AssertionError();
      }
    }
  }
}
