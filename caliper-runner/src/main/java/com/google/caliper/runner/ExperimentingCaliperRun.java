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

import static com.google.common.util.concurrent.Futures.catchingAsync;
import static com.google.common.util.concurrent.Futures.transformAsync;
import static com.google.common.util.concurrent.MoreExecutors.directExecutor;
import static java.util.logging.Level.WARNING;

import com.google.caliper.api.ResultProcessor;
import com.google.caliper.api.SkipThisScenarioException;
import com.google.caliper.core.BenchmarkClassModel;
import com.google.caliper.core.InvalidBenchmarkException;
import com.google.caliper.core.UserCodeException;
import com.google.caliper.model.Measurement;
import com.google.caliper.runner.experiment.Experiment;
import com.google.caliper.runner.experiment.ExperimentSelector;
import com.google.caliper.runner.instrument.Instrument;
import com.google.caliper.runner.instrument.Instrument.InstrumentedMethod;
import com.google.caliper.runner.options.CaliperOptions;
import com.google.caliper.runner.target.Target;
import com.google.caliper.runner.worker.ProxyWorkerException;
import com.google.caliper.runner.worker.WorkerRunner;
import com.google.caliper.runner.worker.dryrun.DryRunComponent;
import com.google.caliper.runner.worker.trial.ScheduledTrial;
import com.google.caliper.runner.worker.trial.TrialComponent;
import com.google.caliper.runner.worker.trial.TrialFailureException;
import com.google.caliper.runner.worker.trial.TrialResult;
import com.google.caliper.runner.worker.trial.TrialSchedulingPolicy;
import com.google.caliper.util.Stdout;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Function;
import com.google.common.base.Optional;
import com.google.common.base.Stopwatch;
import com.google.common.base.Throwables;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableSetMultimap;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Multimap;
import com.google.common.collect.Queues;
import com.google.common.util.concurrent.AsyncFunction;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.MoreExecutors;
import com.google.common.util.concurrent.SettableFuture;
import com.google.common.util.concurrent.Uninterruptibles;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutionException;
import java.util.logging.Logger;
import javax.inject.Inject;
import javax.inject.Provider;

/** An execution of each {@link Experiment} for the configured number of trials. */
@VisibleForTesting
public final class ExperimentingCaliperRun implements CaliperRun {

  private static final Logger logger = Logger.getLogger(ExperimentingCaliperRun.class.getName());

  private static final AsyncFunction<Throwable, Object> FALLBACK_TO_NULL =
      new AsyncFunction<Throwable, Object>() {
        final ListenableFuture<Object> nullFuture = Futures.immediateFuture(null);

        @Override
        public ListenableFuture<Object> apply(Throwable t) throws Exception {
          return nullFuture;
        }
      };

  private final CaliperOptions options;
  private final PrintWriter stdout;
  private final BenchmarkClassModel benchmarkClass;
  private final ImmutableSet<Instrument> instruments;
  private final ImmutableSet<ResultProcessor> resultProcessors;
  private final ExperimentSelector selector;
  private final Provider<ListeningExecutorService> executorProvider;

  private final Provider<DryRunComponent.Builder> dryRunComponentBuilder;
  private final Provider<TrialComponent.Builder> trialComponentBuilder;

  @Inject
  @VisibleForTesting
  public ExperimentingCaliperRun(
      CaliperOptions options,
      @Stdout PrintWriter stdout,
      BenchmarkClassModel benchmarkClass,
      ImmutableSet<Instrument> instruments,
      ImmutableSet<ResultProcessor> resultProcessors,
      ExperimentSelector selector,
      Provider<ListeningExecutorService> executorProvider,
      Provider<DryRunComponent.Builder> dryRunComponentBuilder,
      Provider<TrialComponent.Builder> trialComponentBuilder) {
    this.options = options;
    this.stdout = stdout;
    this.benchmarkClass = benchmarkClass;
    this.instruments = instruments;
    this.resultProcessors = resultProcessors;
    this.selector = selector;
    this.executorProvider = executorProvider;
    this.dryRunComponentBuilder = dryRunComponentBuilder;
    this.trialComponentBuilder = trialComponentBuilder;
  }

  @Override
  public void run() throws InvalidBenchmarkException {
    ImmutableSet<Experiment> allExperiments = selector.selectExperiments();

    printRunInfo(allExperiments);

    if (allExperiments.isEmpty()) {
      throw new InvalidBenchmarkException(
          "There were no experiments to be performed for the class '%s' using the instruments %s",
          benchmarkClass.name(), instruments);
    }

    stdout.format("This selection yields %s experiments.%n", allExperiments.size());
    stdout.flush();

    // always dry run first.
    ImmutableSet<Experiment> experimentsToRun = dryRun(allExperiments);
    if (experimentsToRun.size() != allExperiments.size()) {
      stdout.format(
          "%d experiments were skipped.%n", allExperiments.size() - experimentsToRun.size());
    }

    if (experimentsToRun.isEmpty()) {
      throw new InvalidBenchmarkException("All experiments were skipped.");
    }

    if (options.dryRun()) {
      stdout.println("Dry-run completed successfully.");
      return;
    }

    stdout.flush();

    int totalTrials = experimentsToRun.size() * options.trialsPerScenario();
    Stopwatch stopwatch = Stopwatch.createStarted();
    List<ScheduledTrial> trials = createScheduledTrials(experimentsToRun, totalTrials);
    Multimap<InstrumentedMethod, TrialResult> resultsByInstrumentedMethod = HashMultimap.create();
    final ListeningExecutorService executor = executorProvider.get();
    List<ListenableFuture<TrialResult>> pendingTrials = scheduleTrials(trials, executor);
    ConsoleOutput output = new ConsoleOutput(stdout, totalTrials, stopwatch);
    try {
      // Process results as they complete.
      for (ListenableFuture<TrialResult> trialFuture : inCompletionOrder(pendingTrials)) {
        try {
          TrialResult result = trialFuture.get();
          output.processTrial(result);
          for (ResultProcessor resultProcessor : resultProcessors) {
            resultProcessor.processTrial(result.getTrial());
          }
          resultsByInstrumentedMethod.put(result.getExperiment().instrumentedMethod(), result);
        } catch (ExecutionException e) {
          if (e.getCause() instanceof TrialFailureException) {
            output.processFailedTrial((TrialFailureException) e.getCause());
          } else {
            cancelAll(pendingTrials);
            throw Throwables.propagate(e.getCause());
          }
        } catch (InterruptedException e) {
          cancelAll(pendingTrials);
          throw new RuntimeException(e);
        }
      }
      // Allow our instruments to do validation across all trials for a given benchmark
      for (Map.Entry<InstrumentedMethod, Collection<TrialResult>> entry :
          resultsByInstrumentedMethod.asMap().entrySet()) {
        InstrumentedMethod instrumentedMethod = entry.getKey();
        Optional<String> message =
            instrumentedMethod.validateMeasurements(measurements(entry.getValue()));
        if (message.isPresent()) {
          stdout.printf(
              "For %s (%s)%n  %s%n",
              instrumentedMethod.benchmarkMethod().name(),
              instrumentedMethod.instrument().name(),
              message.get());
        }
      }
    } finally {
      executor.shutdown();
      output.close();
    }

    for (ResultProcessor resultProcessor : resultProcessors) {
      try {
        resultProcessor.close();
      } catch (IOException e) {
        logger.log(WARNING, "Could not close a result processor: " + resultProcessor, e);
      }
    }
  }

  private static Iterable<ImmutableList<Measurement>> measurements(Iterable<TrialResult> results) {
    return Iterables.transform(
        results,
        new Function<TrialResult, ImmutableList<Measurement>>() {
          @Override
          public ImmutableList<Measurement> apply(TrialResult result) {
            return result.getTrial().measurements();
          }
        });
  }

  private void printRunInfo(ImmutableSet<Experiment> allExperiments) {
    // TODO(lukes): move this standard-out handling into the ConsoleOutput class?
    // if the user specified a run name, print it first.
    if (!options.runName().isEmpty()) {
      stdout.println("Run: " + options.runName());
    }
    stdout.println("Experiment selection: ");
    stdout.println(
        "  Benchmark Methods:   "
            + FluentIterable.from(allExperiments)
                .transform(
                    new Function<Experiment, String>() {
                      @Override
                      public String apply(Experiment experiment) {
                        return experiment.instrumentedMethod().benchmarkMethod().name();
                      }
                    })
                .toSet());
    stdout.println(
        "  Instruments:   "
            + FluentIterable.from(selector.instruments())
                .transform(
                    new Function<Instrument, String>() {
                      @Override
                      public String apply(Instrument instrument) {
                        return instrument.name();
                      }
                    }));
    stdout.println("  User parameters:   " + selector.userParameters());
    stdout.println(
        "  Target VMs:  "
            + FluentIterable.from(selector.targets())
                .transform(
                    new Function<Target, String>() {
                      @Override
                      public String apply(Target target) {
                        return target.name();
                      }
                    }));
    stdout.println();
  }

  /**
   * Schedule all the trials.
   *
   * <p>This method arranges all the {@link ScheduledTrial trials} to run according to their
   * scheduling criteria. The executor instance is responsible for enforcing max parallelism.
   */
  private List<ListenableFuture<TrialResult>> scheduleTrials(
      List<ScheduledTrial> trials, final ListeningExecutorService executor) {
    List<ListenableFuture<TrialResult>> pendingTrials = Lists.newArrayList();
    List<ScheduledTrial> serialTrials = Lists.newArrayList();
    for (final ScheduledTrial scheduledTrial : trials) {
      if (scheduledTrial.policy() == TrialSchedulingPolicy.PARALLEL) {
        pendingTrials.add(executor.submit(scheduledTrial.trialTask()));
      } else {
        serialTrials.add(scheduledTrial);
      }
    }
    // A future representing the completion of all prior tasks. Futures.successfulAsList allows us
    // to ignore failure.
    ListenableFuture<?> previous = Futures.successfulAsList(pendingTrials);
    for (final ScheduledTrial scheduledTrial : serialTrials) {
      // each of these trials can only start after all prior trials have finished, so we use
      // Futures.transform to force the sequencing.
      ListenableFuture<TrialResult> current =
          transformAsync(
              previous,
              new AsyncFunction<Object, TrialResult>() {
                @Override
                public ListenableFuture<TrialResult> apply(Object ignored) {
                  return executor.submit(scheduledTrial.trialTask());
                }
              },
              directExecutor());
      pendingTrials.add(current);
      // ignore failure of the prior task.
      previous = catchingAsync(current, Throwable.class, FALLBACK_TO_NULL, directExecutor());
    }
    return pendingTrials;
  }

  /** Returns all the ScheduledTrials for this run. */
  private List<ScheduledTrial> createScheduledTrials(
      ImmutableSet<Experiment> experimentsToRun, int totalTrials) {
    List<ScheduledTrial> trials = Lists.newArrayListWithCapacity(totalTrials);
    /** This is 1-indexed because it's only used for display to users. E.g. "Trial 1 of 27" */
    int trialNumber = 1;
    for (int i = 0; i < options.trialsPerScenario(); i++) {
      for (Experiment experiment : experimentsToRun) {
        ScheduledTrial trial =
            trialComponentBuilder
                .get()
                .trialNumber(trialNumber++)
                .experiment(experiment)
                .build()
                .getScheduledTrial();
        trials.add(trial);
      }
    }
    return trials;
  }

  /**
   * Attempts to run each given experiment once on the target for that experiment. Returns a set of
   * all of the experiments that didn't throw a {@link SkipThisScenarioException}.
   */
  ImmutableSet<Experiment> dryRun(Iterable<Experiment> experiments)
      throws InvalidBenchmarkException {
    // TODO(cgdecker): Use Multimaps.index once lambdas/method references can be used in runner.
    ImmutableSetMultimap<Target, Experiment> experimentsByTarget = indexByTarget(experiments);

    // For now, run dry-runs for each target in serial so we don't have multiple dry-run workers
    // running at the same time. Since currently workers are necessarily all on the same device,
    // creating too many workers at the same time could eat up too much of the system resources or
    // even fail completely (if each worker requests a high -Xms or there are just many workers, for
    // example). Once multiple devices are supported, we should be able to safely run workers on
    // different devices in parallel.
    ImmutableSet.Builder<Experiment> results = ImmutableSet.builder();
    for (Target target : experimentsByTarget.keySet()) {
      WorkerRunner<ImmutableSet<Experiment>> runner =
          dryRunComponentBuilder
              .get()
              .experiments(experimentsByTarget.get(target))
              .build()
              .workerRunner();

      try {
        results.addAll(runner.runWorker());
      } catch (ProxyWorkerException e) {
        if (e.exceptionType().equals(UserCodeException.class.getName())) {
          throw new UserCodeException(e.message(), e);
        } else if (e.exceptionType().equals(InvalidBenchmarkException.class.getName())) {
          throw new InvalidBenchmarkException(e.message(), e);
        }
        throw e;
      }
    }
    return results.build();
  }

  private ImmutableSetMultimap<Target, Experiment> indexByTarget(Iterable<Experiment> experiments) {
    ImmutableSetMultimap.Builder<Target, Experiment> result = ImmutableSetMultimap.builder();
    for (Experiment experiment : experiments) {
      result.put(experiment.target(), experiment);
    }
    return result.build();
  }

  private static void cancelAll(Iterable<? extends ListenableFuture<?>> futures) {
    for (ListenableFuture<?> toCancel : futures) {
      toCancel.cancel(true);
    }
  }

  public static <T> ImmutableList<ListenableFuture<T>> inCompletionOrder(
      Iterable<? extends ListenableFuture<? extends T>> futures) {
    final ConcurrentLinkedQueue<SettableFuture<T>> delegates = Queues.newConcurrentLinkedQueue();
    ImmutableList.Builder<ListenableFuture<T>> listBuilder = ImmutableList.builder();
    for (final ListenableFuture<? extends T> future : futures) {
      SettableFuture<T> delegate = SettableFuture.create();
      // Must make sure to add the delegate to the queue first in case the future is already done
      delegates.add(delegate);
      future.addListener(
          new Runnable() {
            @Override
            public void run() {
              SettableFuture<T> delegate = delegates.remove();
              try {
                delegate.set(Uninterruptibles.getUninterruptibly(future));
              } catch (ExecutionException e) {
                delegate.setException(e.getCause());
              } catch (CancellationException e) {
                delegate.cancel(true);
              }
            }
          },
          MoreExecutors.directExecutor());
      listBuilder.add(delegate);
    }
    return listBuilder.build();
  }
}
