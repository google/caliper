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
import dagger.Subcomponent;

/** Component for creating a {@link ScheduledTrial}. */
@WorkerScoped
@Subcomponent(modules = TrialModule.class)
public interface TrialComponent {
  ScheduledTrial getScheduledTrial();

  /** Builder for {@link TrialComponent}. */
  @Subcomponent.Builder
  interface Builder {
    /** Binds the trial number. */
    @BindsInstance
    Builder trialNumber(@TrialNumber int trialNumber);

    /** Binds the experiment for the trial. */
    @BindsInstance
    Builder experiment(Experiment experiment);

    /** Builds a new {@link TrialComponent}. */
    TrialComponent build();
  }
}
