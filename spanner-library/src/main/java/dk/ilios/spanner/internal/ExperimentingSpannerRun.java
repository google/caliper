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

package dk.ilios.spanner.internal;

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
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.logging.Level;
import java.util.logging.Logger;

import dk.ilios.spanner.Spanner;
import dk.ilios.spanner.output.ResultProcessor;
import dk.ilios.spanner.exception.SkipThisScenarioException;
import dk.ilios.spanner.exception.TrialFailureException;
import dk.ilios.spanner.internal.benchmark.BenchmarkClass;
import dk.ilios.spanner.internal.trial.AndroidUnitTestTrial;
import dk.ilios.spanner.internal.trial.ScheduledTrial;
import dk.ilios.spanner.internal.trial.TrialContext;
import dk.ilios.spanner.model.BenchmarkSpec;
import dk.ilios.spanner.model.Host;
import dk.ilios.spanner.model.InstrumentSpec;
import dk.ilios.spanner.model.Run;
import dk.ilios.spanner.model.Scenario;
import dk.ilios.spanner.model.Trial;
import dk.ilios.spanner.internal.trial.TrialSchedulingPolicy;
import dk.ilios.spanner.log.StdOut;
import dk.ilios.spanner.options.SpannerOptions;

/**
 * An execution of each {@link Experiment} for the configured number of trials.
 */
public final class ExperimentingSpannerRun implements SpannerRun {

    private static final Logger logger = Logger.getLogger(ExperimentingSpannerRun.class.getName());

    private static final FutureFallback<Object> FALLBACK_TO_NULL = new FutureFallback<Object>() {
        final ListenableFuture<Object> nullFuture = Futures.immediateFuture(null);

        @Override
        public ListenableFuture<Object> create(Throwable t) throws Exception {
            return nullFuture;
        }
    };

    private final SpannerOptions options;
    private final StdOut stdout;
    private final Run runInfo;
    private final ImmutableSet<Instrument> instruments;
    private final ImmutableSet<ResultProcessor> resultProcessors;
    private final ExperimentSelector selector;
    private final ListeningExecutorService executorProvider;
    private final Spanner.Callback callback;
    private final Trial[] baselineData;

    public ExperimentingSpannerRun(
            SpannerOptions options,
            StdOut stdout,
            Run runInfo,
            ImmutableSet<Instrument> instruments,
            ImmutableSet<ResultProcessor> resultProcessors,
            ExperimentSelector selector,
            ListeningExecutorService executorProvider,
            Trial[] baselineData,
            Spanner.Callback callback
    ) {
        this.options = options;
        this.stdout = stdout;
        this.runInfo= runInfo;
        this.instruments = instruments;
        this.resultProcessors = resultProcessors;
        this.selector = selector;
        this.executorProvider = executorProvider;
        this.baselineData = baselineData;
        this.callback = callback;
    }


    @Override
    public void run() throws InvalidBenchmarkException {

        ImmutableSet<Experiment> allExperiments = selector.selectExperiments(baselineData);

        // TODO(lukes): move this standard-out handling into the ConsoleOutput class?
        stdout.println("Experiment selection: ");
        stdout.println("  Benchmark Methods:   " + FluentIterable.from(allExperiments)
                .transform(new Function<Experiment, String>() {
                    @Override
                    public String apply(Experiment experiment) {
                        return experiment.instrumentation().benchmarkMethod().getName();
                    }
                }).toSet());
        stdout.println("  Instruments:   " + FluentIterable.from(selector.instruments())
                .transform(new Function<Instrument, String>() {
                    @Override
                    public String apply(Instrument instrument) {
                        return instrument.name();
                    }
                }));
        stdout.println("  User parameters:   " + selector.userParameters());
        stdout.println("  Selection type:    " + selector.selectionType());
        stdout.println();

        stdout.format("This selection yields %s experiments.%n", allExperiments.size());
        stdout.flush();

        // always dry run first.
        ImmutableSet<Experiment> experimentsToRun = dryRun(allExperiments);
//        if (experimentsToRun.size() != allExperiments.size()) {
//            stdout.format("%d experiments were skipped.%n", allExperiments.size() - experimentsToRun.size());
//        }

//        if (experimentsToRun.isEmpty()) {
//            throw new InvalidBenchmarkException("All experiments were skipped.");
//        }
//
//        if (options.dryRun()) {
//            return;
//        }

        stdout.flush();

        int totalTrials = experimentsToRun.size() * options.trialsPerScenario();
        Stopwatch stopwatch = Stopwatch.createStarted();
        List<ScheduledTrial> trials = createScheduledTrials(experimentsToRun, totalTrials);

        List<ListenableFuture<Trial.Result>> pendingTrials = scheduleTrials(trials, executorProvider);
        ConsoleOutput output = new ConsoleOutput(stdout, totalTrials, stopwatch);
        try {
            // Process results as they complete.
            for (ListenableFuture<Trial.Result> trialFuture : inCompletionOrder(pendingTrials)) {
                try {
                    Trial.Result result = trialFuture.get();
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
            executorProvider.shutdown();
            output.close();
        }

        for (ResultProcessor resultProcessor : resultProcessors) {
            try {
                resultProcessor.close();
            } catch (IOException e) {
                logger.log(Level.WARNING, "Could not close a result processor: " + resultProcessor, e);
            }
        }
    }

    private Trial getBaselineData(Trial.Result result) {
        return null;
    }

    /**
     * Schedule all the trials.
     * <p>
     * <p>This method arranges all the {@link ScheduledTrial trials} to run according to their
     * scheduling criteria.  The executor instance is responsible for enforcing max parallelism.
     */
    private List<ListenableFuture<Trial.Result>> scheduleTrials(List<ScheduledTrial> trials,
                                                               final ListeningExecutorService executor) {
        List<ListenableFuture<Trial.Result>> pendingTrials = Lists.newArrayList();
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
            ListenableFuture<Trial.Result> current =
                    Futures.transform(
                            previous,
                            new AsyncFunction<Object, Trial.Result>() {
                                @Override
                                public ListenableFuture<Trial.Result> apply(Object ignored) {
                                    return executor.submit(scheduledTrial.trialTask());
                                }
                            });
            pendingTrials.add(current);
            // ignore failure of the prior task.
            previous = Futures.withFallback(current, FALLBACK_TO_NULL);
        }
        return pendingTrials;
    }

    /**
     * Returns all the ScheduledTrials for this run.
     */
    private List<ScheduledTrial> createScheduledTrials(ImmutableSet<Experiment> experimentsToRun, int totalTrials) {
        List<ScheduledTrial> trials = Lists.newArrayListWithCapacity(totalTrials);
        /** This is 1-indexed because it's only used for display to users.  E.g. "Trial 1 of 27" */
        int trialNumber = 1;
        for (int i = 0; i < options.trialsPerScenario(); i++) {
            for (Experiment experiment : experimentsToRun) {

                BenchmarkSpec benchmarkSpec = experiment.benchmarkSpec();

                MeasurementCollectingVisitor measurementsVisitor = experiment.instrumentation().getMeasurementCollectingVisitor();

                InstrumentSpec instrumentSpec = experiment.instrumentation().instrument().getSpec();

                Scenario scenario = new Scenario.Builder()
                        .benchmarkSpec(benchmarkSpec)
                        .host(new Host.Builder().build())
                        .build();

                // Create a trial from the unique combination of trial number,
                TrialContext trialContext = new TrialContext(UUID.randomUUID(), trialNumber, experiment);
                Trial trial = new Trial.Builder(trialContext)
                        .run(runInfo)
                        .scenario(scenario)
                        .instrumentSpec(instrumentSpec)
                        .build();

                AndroidUnitTestTrial runLoop = new AndroidUnitTestTrial(trial, selector.benchmarkClass(), measurementsVisitor, options, null, callback);
                ScheduledTrial scheduledTrial = new ScheduledTrial(trial, runLoop, TrialSchedulingPolicy.SERIAL);
                trials.add(scheduledTrial);
            }
        }
        return trials;
    }



    /**
     * Attempts to run each given scenario once, in the current VM. Returns a set of all of the
     * scenarios that didn't throw a {@link SkipThisScenarioException}.
     */
    ImmutableSet<Experiment> dryRun(Iterable<Experiment> experiments) throws InvalidBenchmarkException {
        ImmutableSet.Builder<Experiment> builder = ImmutableSet.builder();
        for (Experiment experiment : experiments) {
            try {
                BenchmarkClass benchmarkClass = selector.benchmarkClass();
                Object benchmarkInstance = selector.benchmarkClass().getInstance();
//                benchmarkClass.setUpBenchmark(benchmarkInstance);
                try {
//                    experiment.instrumentation().dryRun(benchmarkInstance);
                    builder.add(experiment);
                } finally {
                    // discard 'benchmark' now; the worker will have to instantiate its own anyway
//                    benchmarkClass.cleanup(benchmarkInstance);
                }
            } catch (SkipThisScenarioException ignoreScenario) {
            }
        }
        return builder.build();
    }

    public static <T> ImmutableList<ListenableFuture<T>> inCompletionOrder(
            Iterable<? extends ListenableFuture<? extends T>> futures) {
        final ConcurrentLinkedQueue<SettableFuture<T>> delegates = Queues.newConcurrentLinkedQueue();
        ImmutableList.Builder<ListenableFuture<T>> listBuilder = ImmutableList.builder();
        Executor executor = MoreExecutors.sameThreadExecutor();
        for (final ListenableFuture<? extends T> future : futures) {
            SettableFuture<T> delegate = SettableFuture.create();
            // Must make sure to add the delegate to the queue first in case the future is already done
            delegates.add(delegate);
            future.addListener(new Runnable() {
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
            }, executor);
            listBuilder.add(delegate);
        }
        return listBuilder.build();
    }
}
