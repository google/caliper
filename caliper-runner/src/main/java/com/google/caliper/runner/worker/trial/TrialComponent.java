/*
 * Copyright (C) 2015 Google Inc.
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

package com.google.caliper.runner.worker.trial;

import com.google.caliper.runner.experiment.Experiment;
import com.google.caliper.runner.worker.WorkerScoped;
import dagger.BindsInstance;
import dagger.producers.Producer;
import dagger.producers.Production;
import dagger.producers.ProductionSubcomponent;
import java.util.concurrent.Executor;

/** Component that produces results for one trial. */
@WorkerScoped
@ProductionSubcomponent(modules = TrialModule.class)
public interface TrialComponent {

  Producer<TrialResult> trialResult();

  /** An object that can run a trial of an {@link Experiment}. */
  interface TrialRunner {
    /**
     * Returns a {@link Producer} that returns the result of running a trial of an {@link
     * Experiment}.
     */
    Producer<TrialResult> trialResultProducer(Experiment experiment, int trialNumber);
  }

  @ProductionSubcomponent.Builder
  abstract class Builder {
    @BindsInstance
    public abstract Builder executor(@Production Executor executor);

    /** Binds the trial number. */
    @BindsInstance
    public abstract Builder trialNumber(@TrialNumber int trialNumber);

    /** Binds the experiment for the trial. */
    @BindsInstance
    public abstract Builder experiment(Experiment experiment);

    public abstract TrialComponent build();

    /** Returns an object that can run trials using a given executor. */
    public final TrialRunner trialRunner(Executor executor) {
      executor(executor);
      return new TrialRunner() {
        @Override
        public Producer<TrialResult> trialResultProducer(Experiment experiment, int trialNumber) {
          return experiment(experiment).trialNumber(trialNumber).build().trialResult();
        }
      };
    }
  }
}
