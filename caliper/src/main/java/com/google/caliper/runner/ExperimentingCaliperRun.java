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

import static com.google.common.base.Preconditions.checkState;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static java.util.concurrent.TimeUnit.NANOSECONDS;
import static java.util.logging.Level.WARNING;

import com.google.caliper.api.ResultProcessor;
import com.google.caliper.api.SkipThisScenarioException;
import com.google.caliper.bridge.AbstractLogMessageVisitor;
import com.google.caliper.bridge.FailureLogMessage;
import com.google.caliper.bridge.LogMessage;
import com.google.caliper.bridge.ShouldContinueMessage;
import com.google.caliper.bridge.StopMeasurementLogMessage;
import com.google.caliper.bridge.VmOptionLogMessage;
import com.google.caliper.bridge.VmPropertiesLogMessage;
import com.google.caliper.model.BenchmarkSpec;
import com.google.caliper.model.Host;
import com.google.caliper.model.Run;
import com.google.caliper.model.Scenario;
import com.google.caliper.model.Trial;
import com.google.caliper.model.VmSpec;
import com.google.caliper.options.CaliperOptions;
import com.google.caliper.runner.Instrument.MeasurementCollectingVisitor;
import com.google.caliper.runner.StreamService.StreamItem;
import com.google.caliper.util.ShortDuration;
import com.google.caliper.util.Stderr;
import com.google.caliper.util.Stdout;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Function;
import com.google.common.base.Optional;
import com.google.common.base.Predicate;
import com.google.common.base.Stopwatch;
import com.google.common.base.Throwables;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Maps;
import com.google.gson.Gson;
import com.google.inject.CreationException;
import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Key;
import com.google.inject.ProvisionException;
import com.google.inject.spi.Message;

import org.joda.time.Duration;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.UUID;
import java.util.logging.Logger;

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
  private final StreamService.Factory streamServiceFactory;
  private final ServerSocketService serverSocketService;
  private final ExperimentSelector selector;
  private final WorkerProcess.Factory workerFactory;
  private final Host host;
  private final Run run;
  private final Gson gson;

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
      StreamService.Factory streamServiceFactory,
      ServerSocketService serverSocketService,
      WorkerProcess.Factory workerFactory,
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
    this.streamServiceFactory = streamServiceFactory;
    this.serverSocketService = serverSocketService;
    this.workerFactory = workerFactory;
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

    for (int i = 0; i < options.trialsPerScenario(); i++) {
      for (Experiment experiment : experimentsToRun) {
        stdout.printf("Starting experiment %d of %d: %s\u2026 ",
            trialNumber, totalTrials, experiment);
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

  // TODO(lukes): This method tracks a lot of temporary state, consider extracting a class.
  private Trial measure(final Experiment experiment) throws IOException {
    BenchmarkSpec benchmarkSpec = new BenchmarkSpec.Builder()
        .className(experiment.instrumentation().benchmarkMethod().getDeclaringClass().getName())
        .methodName(experiment.instrumentation().benchmarkMethod().getName())
        .addAllParameters(experiment.userParameters())
        .build();

    UUID trialId = UUID.randomUUID();
    StreamService manager = streamServiceFactory.create(
        workerFactory.create(
            trialId,
            serverSocketService.getConnection(trialId),
            experiment,
            benchmarkSpec),
        trialNumber);
    trialStopwatch.start();
    manager.start();
    try {
      // TODO(lukes): The DataCollectingVisitor should be able to tell us when it has collected all
      // its data.
      DataCollectingVisitor dataCollectingVisitor = new DataCollectingVisitor();
      MeasurementCollectingVisitor measurementCollectingVisitor = 
          experiment.instrumentation().getMeasurementCollectingVisitor();
      
      long timeLimitNanos = getTrialTimeLimitTrialNanos();
      boolean doneCollecting = false;
      boolean done = false;
      while (!done) {
        StreamItem item = manager.readItem(timeLimitNanos - trialStopwatch.elapsed(NANOSECONDS), 
            NANOSECONDS);
        switch (item.kind()) {
          case DATA:
            LogMessage logMessage = item.content();
            logMessage.accept(measurementCollectingVisitor);
            logMessage.accept(dataCollectingVisitor);
            if (!doneCollecting && measurementCollectingVisitor.isDoneCollecting()) {
              doneCollecting = true;
              // We have received all the measurements we need and are about to tell the worker to
              // shut down.  At this point the worker should shutdown soon, but we don't want to 
              // wait too long, so decrease the time limit so that we wait no more than 
              // WORKER_CLEANUP_DURATION.
              long cleanupTimeNanos = MILLISECONDS.toNanos(WORKER_CLEANUP_DURATION.getMillis());
              // TODO(lukes): Does the min operation make sense here? should we just use the 
              // cleanupTimeNanos?
              timeLimitNanos = trialStopwatch.elapsed(NANOSECONDS) + cleanupTimeNanos;
            }
            // If it is a stop measurement message we need to tell the worker to either stop or keep
            // going with a WorkerContinueMessage.  This needs to be done after the 
            // measurementCollecting visitor sees the message so that isDoneCollection will be up to
            // date.
            if (logMessage instanceof StopMeasurementLogMessage) {
              // TODO(lukes): this is a blocking write, perhaps we should perform it in a non 
              // blocking manner to keep this thread only blocking in one place.  This would 
              // complicate error handling, but may increase performance since it would free this
              // thread up to handle other messages.
              manager.writeLine(gson.toJson(new ShouldContinueMessage(!doneCollecting)));
              if (doneCollecting) {
                manager.closeWriter();
              }
            }
            break;
          case EOF:
            // We consider EOF to be synonymous with worker shutdown
            if (!doneCollecting) {
              throw new TrialFailureException("The worker exited without producing data. It has "
                  + "likely crashed. Run with --verbose to see any worker output.");
            }
            done = true;
            break;
          case TIMEOUT:
            if (doneCollecting) {
              // Should this be an error?
              logger.warning("Worker failed to exit cleanly within the alloted time.");  
              done = true;
            } else {
              throw new TrialFailureException(String.format(
                  "Trial exceeded the total allowable runtime (%s). "
                      + "The limit may be adjusted using the --time-limit flag.",
                      options.timeLimit()));
            }
            break;
          default:
            throw new AssertionError("Impossible item: " + item);
        }
      }

      return makeTrial(trialId, experiment, benchmarkSpec, dataCollectingVisitor, 
          measurementCollectingVisitor);
    } catch (InterruptedException e) {
      throw new AssertionError();
    } finally {
      trialStopwatch.reset();
      manager.stop();
    }
  }

  /** Returns a {@link Trial} containing all the data collected by the given experiment. */
  private Trial makeTrial(UUID trialId, final Experiment experiment, BenchmarkSpec benchmarkSpec,
      DataCollectingVisitor dataCollectingVisitor,
      MeasurementCollectingVisitor measurementCollectingVisitor) {
    checkState(measurementCollectingVisitor.isDoneCollecting());
    ImmutableMap<String, String> vmOptions = dataCollectingVisitor.vmOptionsBuilder.build();
    checkState(!vmOptions.isEmpty());
    VmSpec vmSpec = new VmSpec.Builder()
        .addAllProperties(dataCollectingVisitor.vmProperties.get())
        .addAllOptions(vmOptions)
        .build();
    return new Trial.Builder(trialId)
        .run(run)
        .instrumentSpec(experiment.instrumentation().instrument().getSpec())
        .scenario(new Scenario.Builder()
            .host(host)
            .vmSpec(vmSpec)
            .benchmarkSpec(benchmarkSpec))
        .addAllMeasurements(measurementCollectingVisitor.getMeasurements())
        .build();
  }

  private long getTrialTimeLimitTrialNanos() {
    ShortDuration timeLimit = options.timeLimit();
    if (ShortDuration.zero().equals(timeLimit)) {
      return Long.MAX_VALUE;
    }
    return timeLimit.to(NANOSECONDS);
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
}
