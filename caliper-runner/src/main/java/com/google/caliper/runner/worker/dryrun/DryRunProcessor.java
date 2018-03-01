/*
 * Copyright (C) 2018 Google Inc.
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

package com.google.caliper.runner.worker.dryrun;

import static java.util.concurrent.TimeUnit.MINUTES;

import com.google.caliper.bridge.AbstractLogMessageVisitor;
import com.google.caliper.bridge.DryRunSuccessLogMessage;
import com.google.caliper.bridge.LogMessage;
import com.google.caliper.runner.experiment.Experiment;
import com.google.caliper.runner.worker.FailureLogMessageVisitor;
import com.google.caliper.runner.worker.Worker;
import com.google.caliper.runner.worker.WorkerProcessor;
import com.google.caliper.util.ShortDuration;
import com.google.common.base.Predicates;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Maps;
import java.util.Set;
import javax.annotation.Nullable;
import javax.inject.Inject;

/**
 * {@link WorkerProcessor} for receiving a set of experiment IDs from a dry-run worker and
 * converting them to a set of experiments.
 *
 * @author Colin Decker
 */
final class DryRunProcessor extends WorkerProcessor<ImmutableSet<Experiment>> {

  private final SuccessVisitor successVisitor;

  @Inject
  DryRunProcessor(Set<Experiment> experiments) {
    this.successVisitor = new SuccessVisitor(experiments);
  }

  @Override
  public ShortDuration timeLimit() {
    // Could be doing dry-runs of a lot of benchmarks on one target, so allow plenty of time.
    // TODO(cgdecker): Should this be made configurable, like the trial time limit? Or should we
    // use the trial time limit times the number of experiments being run on the worker?
    return ShortDuration.of(10, MINUTES);
  }

  @Override
  public boolean handleMessage(LogMessage message, Worker worker) {
    message.accept(FailureLogMessageVisitor.INSTANCE);
    message.accept(successVisitor);
    return successVisitor.result != null;
  }

  @Override
  public ImmutableSet<Experiment> getResult() {
    return successVisitor.result;
  }

  private static final class SuccessVisitor extends AbstractLogMessageVisitor {

    private final ImmutableMap<Integer, Experiment> experiments;
    @Nullable private volatile ImmutableSet<Experiment> result = null;

    private SuccessVisitor(Iterable<Experiment> experiments) {
      ImmutableMap.Builder<Integer, Experiment> builder = ImmutableMap.builder();
      for (Experiment experiment : experiments) {
        builder.put(experiment.id(), experiment);
      }
      this.experiments = builder.build();
    }

    @Override
    public void visit(DryRunSuccessLogMessage logMessage) {
      this.result =
          ImmutableSet.copyOf(
              Maps.filterKeys(experiments, Predicates.in(logMessage.ids())).values());
    }
  }
}
