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

import static java.util.concurrent.TimeUnit.NANOSECONDS;
import static java.util.logging.Level.WARNING;

import com.google.caliper.api.ResultProcessor;
import com.google.caliper.api.SkipThisScenarioException;
import com.google.caliper.model.Trial;
import com.google.caliper.options.CaliperOptions;
import com.google.caliper.util.ShortDuration;
import com.google.caliper.util.Stderr;
import com.google.caliper.util.Stdout;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Function;
import com.google.common.base.Stopwatch;
import com.google.common.base.Throwables;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableSet;
import com.google.inject.CreationException;
import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Key;
import com.google.inject.ProvisionException;
import com.google.inject.spi.Message;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.logging.Logger;

import javax.inject.Provider;

/**
 * An execution of each {@link Experiment} for the configured number of trials.
 */
@VisibleForTesting
public final class ExperimentingCaliperRun implements CaliperRun {

  private static final Logger logger = Logger.getLogger(ExperimentingCaliperRun.class.getName());

  private final Injector injector;
  private final CaliperOptions options;
  private final PrintWriter stdout;
  private final PrintWriter stderr;
  private final BenchmarkClass benchmarkClass;
  private final ImmutableSet<Instrument> instruments;
  private final ImmutableSet<ResultProcessor> resultProcessors;
  private final ExperimentSelector selector;
  private final Provider<TrialRunLoop> runLoopProvider;

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
      ExperimentSelector selector,
      Provider<TrialRunLoop> runLoopProvider) {
    this.injector = injector;
    this.options = options;
    this.stdout = stdout;
    this.stderr = stderr;
    this.benchmarkClass = benchmarkClass;
    this.instruments = instruments;
    this.resultProcessors = resultProcessors;
    this.runLoopProvider = runLoopProvider;
    this.selector = selector;
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
    Stopwatch stopwatch = Stopwatch.createStarted();

    for (int i = 0; i < options.trialsPerScenario(); i++) {
      for (Experiment experiment : experimentsToRun) {
        stdout.printf("Starting experiment %d of %d: %s\u2026 ",
            trialNumber, totalTrials, experiment);
        try {
          Trial trial = TrialScopes.makeContext(UUID.randomUUID(), trialNumber, experiment)
              .call(new Callable<Trial>() {
                @Override public Trial call() throws Exception {
                  return runLoopProvider.get().call();
                }
              });
          stdout.println("Complete!");
          for (ResultProcessor resultProcessor : resultProcessors) {
            resultProcessor.processTrial(trial);
          }
        } catch (TrialFailureException e) {
          stderr.println(
              "ERROR: Trial failed to complete (its results will not be included in the run):\n"
                  + "  " + e.getMessage());
        } catch (Exception e) {
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
}
