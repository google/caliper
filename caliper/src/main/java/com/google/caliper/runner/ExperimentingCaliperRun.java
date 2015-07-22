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

import static java.util.logging.Level.WARNING;

import com.google.caliper.api.ResultProcessor;
import com.google.caliper.api.SkipThisScenarioException;
import com.google.caliper.options.CaliperOptions;
import com.google.caliper.util.Stdout;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Function;
import com.google.common.base.Stopwatch;
import com.google.common.base.Throwables;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import com.google.common.collect.Queues;
import com.google.common.util.concurrent.AsyncFunction;
import com.google.common.util.concurrent.FutureFallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.MoreExecutors;
import com.google.common.util.concurrent.SettableFuture;
import com.google.common.util.concurrent.Uninterruptibles;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutionException;
import java.util.logging.Logger;

import javax.inject.Inject;
import javax.inject.Provider;

/**
 * An execution of each {@link Experiment} for the configured number of trials.
 */
@VisibleForTesting
public final class ExperimentingCaliperRun implements CaliperRun {

  private static final Logger logger = Logger.getLogger(ExperimentingCaliperRun.class.getName());

  private static final FutureFallback<Object> FALLBACK_TO_NULL = new FutureFallback<Object>() {
    final ListenableFuture<Object> nullFuture = Futures.immediateFuture(null);
    @Override public ListenableFuture<Object> create(Throwable t) throws Exception {
      return nullFuture;
    }
  };

  private final MainComponent mainComponent;
  private final CaliperOptions options;
  private final PrintWriter stdout;
  private final BenchmarkClass benchmarkClass;
  private final ImmutableSet<Instrument> instruments;
  private final ImmutableSet<ResultProcessor> resultProcessors;
  private final ExperimentSelector selector;
  private final Provider<ListeningExecutorService> executorProvider;

  @Inject @VisibleForTesting
  public ExperimentingCaliperRun(
      MainComponent mainComponent,
      CaliperOptions options,
      @Stdout PrintWriter stdout,
      BenchmarkClass benchmarkClass,
      ImmutableSet<Instrument> instruments,
      ImmutableSet<ResultProcessor> resultProcessors,
      ExperimentSelector selector,
      Provider<ListeningExecutorService> executorProvider) {
    this.mainComponent = mainComponent;
    this.options = options;
    this.stdout = stdout;
    this.benchmarkClass = benchmarkClass;
    this.instruments = instruments;
    this.resultProcessors = resultProcessors;
    this.selector = selector;
    this.executorProvider = executorProvider;
  }

  @Override
  public void run() throws InvalidBenchmarkException {
    ImmutableSet<Experiment> allExperiments = selector.selectExperiments();
    // TODO(lukes): move this standard-out handling into the ConsoleOutput class?
    stdout.println("Experiment selection: ");
    stdout.println("  Benchmark Methods:   " + FluentIterable.from(allExperiments)
        .transform(new Function<Experiment, String>() {
          @Override public String apply(Experiment experiment) {
            return experiment.instrumentation().benchmarkMethod().getName();
          }
        }).toSet());
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
    Stopwatch stopwatch = Stopwatch.createStarted();
    List<ScheduledTrial> trials = createScheduledTrials(experimentsToRun, totalTrials);

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
        } catch (ExecutionException e) {
          if (e.getCause() instanceof TrialFailureException) {
            output.processFailedTrial((TrialFailureException) e.getCause());
          } else {
            for (ListenableFuture<?> toCancel : pendingTrials) {
              toCancel.cancel(true);
            }
            throw Throwables.propagate(e.getCause());
          }
        } catch (InterruptedException e) {
          // be responsive to interruption, cancel outstanding work and exit
          for (ListenableFuture<?> toCancel : pendingTrials) {
            // N.B. TrialRunLoop is responsive to interruption.
            toCancel.cancel(true);
          }
          throw new RuntimeException(e);
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

  /**
   * Schedule all the trials.
   *
   * <p>This method arranges all the {@link ScheduledTrial trials} to run according to their
   * scheduling criteria.  The executor instance is responsible for enforcing max parallelism.
   */
  private List<ListenableFuture<TrialResult>> scheduleTrials(List<ScheduledTrial> trials,
      final ListeningExecutorService executor) {
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
          Futures.transform(
              previous,
              new AsyncFunction<Object, TrialResult>() {
                @Override public ListenableFuture<TrialResult> apply(Object ignored) {
                  return executor.submit(scheduledTrial.trialTask());
                }
              });
      pendingTrials.add(current);
      // ignore failure of the prior task.
      previous = Futures.withFallback(current, FALLBACK_TO_NULL);
    }
    return pendingTrials;
  }

  /** Returns all the ScheduledTrials for this run. */
  private List<ScheduledTrial> createScheduledTrials(ImmutableSet<Experiment> experimentsToRun,
      int totalTrials) {
    List<ScheduledTrial> trials = Lists.newArrayListWithCapacity(totalTrials);
    /** This is 1-indexed because it's only used for display to users.  E.g. "Trial 1 of 27" */
    int trialNumber = 1;
    for (int i = 0; i < options.trialsPerScenario(); i++) {
      for (Experiment experiment : experimentsToRun) {
        try {
          TrialScopeComponent trialScopeComponent = mainComponent.newTrialComponent(
              new TrialModule(UUID.randomUUID(), trialNumber, experiment));

          trials.add(trialScopeComponent.getScheduledTrial());
        } finally {
          trialNumber++;
        }
      }
    }
    return trials;
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
        ExperimentComponent experimentComponent =
            mainComponent.newExperimentComponent(ExperimentModule.forExperiment(experiment));
        Object benchmark = experimentComponent.getBenchmarkInstance();
        benchmarkClass.setUpBenchmark(benchmark);
        try {
          experiment.instrumentation().dryRun(benchmark);
          builder.add(experiment);
        } finally {
          // discard 'benchmark' now; the worker will have to instantiate its own anyway
          benchmarkClass.cleanup(benchmark);
        }
      } catch (SkipThisScenarioException innocuous) {}
    }
    return builder.build();
  }

  public static <T> ImmutableList<ListenableFuture<T>> inCompletionOrder(
      Iterable<? extends ListenableFuture<? extends T>> futures) {
    final ConcurrentLinkedQueue<SettableFuture<T>> delegates = Queues.newConcurrentLinkedQueue();
    ImmutableList.Builder<ListenableFuture<T>> listBuilder = ImmutableList.builder();
    for (final ListenableFuture<? extends T> future : futures) {
      SettableFuture<T> delegate = SettableFuture.create();
      // Must make sure to add the delegate to the queue first in case the future is already done
      delegates.add(delegate);
      future.addListener(new Runnable() {
        @Override public void run() {
          SettableFuture<T> delegate = delegates.remove();
          try {
            delegate.set(Uninterruptibles.getUninterruptibly(future));
          } catch (ExecutionException e) {
            delegate.setException(e.getCause());
          } catch (CancellationException e) {
            delegate.cancel(true);
          }
        }
      }, MoreExecutors.directExecutor());
      listBuilder.add(delegate);
    }
    return listBuilder.build();
  }
}
