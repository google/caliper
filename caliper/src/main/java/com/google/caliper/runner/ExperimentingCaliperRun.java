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
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static java.util.concurrent.TimeUnit.NANOSECONDS;
import static java.util.logging.Level.SEVERE;
import static java.util.logging.Level.WARNING;

import com.google.caliper.api.ResultProcessor;
import com.google.caliper.api.SkipThisScenarioException;
import com.google.caliper.bridge.AbstractLogMessageVisitor;
import com.google.caliper.bridge.CaliperControlLogMessage;
import com.google.caliper.bridge.FailureLogMessage;
import com.google.caliper.bridge.LogMessage;
import com.google.caliper.bridge.LogMessageVisitor;
import com.google.caliper.bridge.ShouldContinueMessage;
import com.google.caliper.bridge.StopMeasurementLogMessage;
import com.google.caliper.bridge.VmOptionLogMessage;
import com.google.caliper.bridge.VmPropertiesLogMessage;
import com.google.caliper.bridge.WorkerSpec;
import com.google.caliper.model.BenchmarkSpec;
import com.google.caliper.model.Host;
import com.google.caliper.model.Run;
import com.google.caliper.model.Scenario;
import com.google.caliper.model.Trial;
import com.google.caliper.model.VmSpec;
import com.google.caliper.options.CaliperOptions;
import com.google.caliper.runner.Instrument.Instrumentation;
import com.google.caliper.runner.Instrument.MeasurementCollectingVisitor;
import com.google.caliper.util.Parser;
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
import com.google.common.collect.Iterables;
import com.google.common.collect.Maps;
import com.google.common.collect.Queues;
import com.google.common.io.Closeables;
import com.google.common.io.LineReader;
import com.google.common.util.concurrent.AsyncFunction;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.MoreExecutors;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.google.gson.Gson;
import com.google.inject.CreationException;
import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Key;
import com.google.inject.ProvisionException;
import com.google.inject.spi.Message;

import org.joda.time.Duration;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.Reader;
import java.io.Writer;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.Charset;
import java.text.ParseException;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.annotation.Nullable;

/**
 * An execution of each {@link Experiment} for the configured number of trials.
 */
@VisibleForTesting
public final class ExperimentingCaliperRun implements CaliperRun {
  /** The time that the worker has to clean up after an experiment. */
  private static final Duration WORKER_CLEANUP_DURATION = Duration.standardSeconds(2);

  private static final Logger logger = Logger.getLogger(ExperimentingCaliperRun.class.getName());

  private final Injector injector;
  private final CaliperOptions options;
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

  private final Stopwatch trialStopwatch = new Stopwatch();
  /** This is 1-indexed because it's only used for display to users.  E.g. "Trial 1 of 27" */
  private volatile int trialNumber = 1;

  @Inject @VisibleForTesting
  public ExperimentingCaliperRun(
      Injector injector,
      CaliperOptions options,
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
    this.injector = injector;
    this.options = options;
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
          "There were no experiments to be performed for the class %s using the instruments %s",
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
      throw new InvalidBenchmarkException("All experiments were skipped.");
    }

    if (options.dryRun()) {
      return;
    }

    stdout.flush();

    int totalTrials = experimentsToRun.size() * options.trialsPerScenario();
    Stopwatch stopwatch = new Stopwatch().start();

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
      BenchmarkSpec benchmarkSpec, int port) {
    Instrumentation instrumentation = experiment.instrumentation();
    Instrument instrument = instrumentation.instrument();
    ImmutableList.Builder<String> parameterClassNames = ImmutableList.builder();
    for (Class<?> parameterType : instrumentation.benchmarkMethod.getParameterTypes()) {
      parameterClassNames.add(parameterType.getName());
    }
    WorkerSpec request = new WorkerSpec(
        instrumentation.workerClass().getName(),
        instrumentation.workerOptions(),
        benchmarkSpec,
        parameterClassNames.build(),
        port);

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

    String classPath = getClassPath();
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

  private static String getClassPath() {
    String classpath;
    // Use the effective class path in case this is being invoked in an isolated class loader
    classpath =
        EffectiveClassPath.getClassPathForClassLoader(Thread.currentThread().getContextClassLoader());
    return classpath;
  }


  private Trial measure(final Experiment experiment) throws IOException {
    BenchmarkSpec benchmarkSpec = new BenchmarkSpec.Builder()
        .className(experiment.instrumentation().benchmarkMethod().getDeclaringClass().getName())
        .methodName(experiment.instrumentation().benchmarkMethod().getName())
        .addAllParameters(experiment.userParameters())
        .build();

    final ServerSocket serverSocket = new ServerSocket(0);

    final WorkerProcess process =
        new WorkerProcess(createWorkerProcessBuilder(experiment, benchmarkSpec,
            serverSocket.getLocalPort()));
    final ListenableFuture<Integer> processFuture = processExecutor.submit(new Callable<Integer>() {
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
      // TODO(gak,lukes): rewrite to encapsulate the worker in a service that exposes stderr and
      // stdout as Pipes so that they can be select()'ed on along with the socket.
      // use the default charset because worker streams will use the default for output
      Charset processCharset = Charset.defaultCharset();
      producerExecutor.submit(
          new LineProducer(new InputStreamReader(process.getInputStream(), processCharset), queue));
      producerExecutor.submit(
          new LineProducer(new InputStreamReader(process.getErrorStream(), processCharset), queue));
      final ListenableFuture<Socket> pipeFuture =
          producerExecutor.submit(new Callable<Socket>() {
            @Override public Socket call() throws IOException {
              Socket socket = serverSocket.accept();
              // See comment in WorkerModule for why this is necessary.
              socket.setTcpNoDelay(true);
              return socket;
            }
          });
      final ListenableFuture<Reader> pipeReaderFuture = Futures.transform(pipeFuture,
          new AsyncFunction<Socket, Reader>() {
            @Override public ListenableFuture<Reader> apply(Socket pipe) throws IOException {
              return Futures.<Reader>immediateFuture(
                  new InputStreamReader(pipe.getInputStream(), UTF_8));
            }
          });
      final ListenableFuture<Writer> pipeWriterFuture = Futures.transform(pipeFuture,
          new AsyncFunction<Socket, Writer>() {
            @Override public ListenableFuture<Writer> apply(Socket pipe) throws IOException {
              return Futures.<Writer>immediateFuture(
                  new OutputStreamWriter(pipe.getOutputStream(), UTF_8));
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

      final DataCollectingVisitor dataCollectingVisitor = new DataCollectingVisitor();
      ListenableFuture<MeasurementCollectingVisitor> consumerFuture =
          Futures.transform(pipeWriterFuture,
              new AsyncFunction<Writer, MeasurementCollectingVisitor>() {
                @Override public ListenableFuture<MeasurementCollectingVisitor> apply(Writer input)
                    throws Exception {
                  return consumerExecutor.submit(
                      new LineConsumer(queue,
                          new BufferedWriter(input),
                          experiment.instrumentation().getMeasurementCollectingVisitor(),
                          ImmutableSet.of(dataCollectingVisitor)));
                }
              });
      /*
       * Start watching the queue before we wait on the pipe so that we can see output from failed
       * workers.
       */
      consumerFuture.addListener(new Runnable() {
        @Override public void run() {
          // kill the process because we're all done
          try {
            // by closing the pipe, the worker should notice and have a chance shut itself down
            // cleanly.
            Futures.getUnchecked(pipeFuture).close();
            // wait for no more than 2 seconds before killing the worker.
            processFuture.get(WORKER_CLEANUP_DURATION.getMillis(), MILLISECONDS);
          } catch (Exception e) {
            // Ignore, this is a best effort only attempt to quietly shutdown the worker.
            logger.log(Level.FINE, "Exception while waiting for the worker to stop.", e);
          } finally {
            try {
              process.exitValue();
              // process has already exited
            } catch (IllegalThreadStateException e) {
              logger.warning("The worker did not exit within " + WORKER_CLEANUP_DURATION.getMillis()
                  + " milliseconds.  Forcefully killing worker.");
              process.destroy();
            }
          }
        }
      }, MoreExecutors.sameThreadExecutor());

      process.waitFor();
      // We call get() here so that dataCollectingVisitor is definitely visible since all writes to
      // it occur in the consumer thread.
      MeasurementCollectingVisitor measurementCollectingVisitor = consumerFuture.get();

      ImmutableMap<String, String> vmOptions = dataCollectingVisitor.vmOptionsBuilder.build();
      checkState(!vmOptions.isEmpty());
      VmSpec vmSpec = new VmSpec.Builder()
          .addAllProperties(dataCollectingVisitor.vmProperties.get())
          .addAllOptions(vmOptions)
          .build();
      return new Trial.Builder(UUID.randomUUID())
          .run(run)
          .instrumentSpec(experiment.instrumentation().instrument().getSpec())
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
      throw Throwables.propagate(cause);
    } finally {
      trialStopwatch.reset();
      producerExecutor.shutdownNow();
      serverSocket.close();
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
      Class<?> clazz = benchmarkClass.benchmarkClass();
      try {
        Object benchmark = injector.createChildInjector(ExperimentModule.forExperiment(experiment))
            .getInstance(Key.get(clazz));
        benchmarkClass.setUpBenchmark(benchmark);
        try {
          experiment.instrumentation().dryRun(benchmark);
          builder.add(experiment);
        } finally {
          // discard 'benchmark' now; the worker will have to instantiate its own anyway
          benchmarkClass.cleanup(benchmark);
        }
      } catch (ProvisionException e) {
        Throwable cause = e.getCause();
        if (cause != null) {
          throw new UserCodeException(cause);
        }
        throw e;
      } catch (CreationException e) {
        // Guice formatting is a little ugly
        StringBuilder message = new StringBuilder(
            "Could not create an instance of the benchmark class following reasons:");
        int errorNum = 0;
        for (Message guiceMessage : e.getErrorMessages()) {
          message.append("\n  ").append(++errorNum).append(") ")
              .append(guiceMessage.getMessage());
        }
        throw new InvalidBenchmarkException(message.toString(), e);
      } catch (SkipThisScenarioException innocuous) {}
    }
    return builder.build();
  }

  private static final class DataCollectingVisitor extends AbstractLogMessageVisitor {
    final ImmutableMap.Builder<String, String> vmOptionsBuilder = ImmutableMap.builder();
    Optional<ImmutableMap<String, String>> vmProperties = Optional.absent();

    @Override
    public void visit(FailureLogMessage logMessage) {
      throw new ProxyWorkerException(logMessage.stackTrace());
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

  private final class LineConsumer implements Callable<MeasurementCollectingVisitor> {
    final BlockingQueue<String> queue;
    final MeasurementCollectingVisitor measurementCollectingVisitor;
    final ImmutableSet<? extends LogMessageVisitor> otherVisitors;
    final Writer workerWriter;

    LineConsumer(BlockingQueue<String> queue,
        Writer workerWriter, MeasurementCollectingVisitor measurementCollectingVisitor,
        ImmutableSet<? extends LogMessageVisitor> otherVisitors) {
      this.queue = queue;
      this.measurementCollectingVisitor = measurementCollectingVisitor;
      this.otherVisitors = otherVisitors;
      this.workerWriter = workerWriter;
    }

    @Override public MeasurementCollectingVisitor call() throws InterruptedException, IOException {
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

      return measurementCollectingVisitor;
    }

    void processLine(String line) throws IOException {
      try {
        LogMessage logMessage = logMessageParser.parse(line);
        if (options.verbose() && !(logMessage instanceof CaliperControlLogMessage)) {
          stdout.printf("[trial-%d] %s%n", trialNumber, line);
        }
        logMessage.accept(measurementCollectingVisitor);
        for (LogMessageVisitor visitor : otherVisitors) {
          logMessage.accept(visitor);
        }
        // If it is a stop measurement message we need to tell the worker to either stop or keep
        // going with a WorkerContinueMessage.  This needs to be done after the
        // measurementCollecting visitor sees the message so that isDoneCollection will be up to
        // date.
        if (logMessage instanceof StopMeasurementLogMessage) {
          workerWriter.write(gson.toJson(
              new ShouldContinueMessage(!measurementCollectingVisitor.isDoneCollecting())));
          workerWriter.write('\n');
          workerWriter.flush();
        }
      } catch (ParseException e) {
        throw new AssertionError();
      }
    }
  }
}
