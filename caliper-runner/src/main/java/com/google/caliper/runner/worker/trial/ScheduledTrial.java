/*
 * Copyright (C) 2014 Google Inc.
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

package com.google.caliper.runner.worker.trial;

import com.google.caliper.runner.worker.WorkerRunner;
import java.util.concurrent.Callable;
import javax.inject.Inject;

/**
 * A simple pair of a {@link WorkerRunner WorkerRunner<TrialResult>} and a {@link
 * TrialSchedulingPolicy}.
 */
public final class ScheduledTrial {
  private final WorkerRunner<TrialResult> trialTask;
  private final TrialSchedulingPolicy policy;

  @Inject
  ScheduledTrial(WorkerRunner<TrialResult> trialTask, TrialSchedulingPolicy policy) {
    this.trialTask = trialTask;
    this.policy = policy;
  }

  public TrialSchedulingPolicy policy() {
    return policy;
  }

  public Callable<TrialResult> trialTask() {
    return trialTask;
  }
}
