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

package dk.ilios.spanner.internal.trial;

import java.util.concurrent.Callable;

import dk.ilios.spanner.model.Trial;

/**
 * A ScheduledTrial is a wrapper around Trial data so it can be executed later.
 */
public final class ScheduledTrial {
    private final Callable<Trial.Result> runLoop;
    private final Trial trial;
    private final TrialSchedulingPolicy policy;

    public ScheduledTrial(Trial trial, Callable<Trial.Result> runLoop, TrialSchedulingPolicy policy) {
        this.trial = trial;
        this.runLoop = runLoop;
        this.policy = policy;
    }

    public TrialSchedulingPolicy policy() {
        return policy;
    }

    public Callable<Trial.Result> trialTask() {
        return runLoop;
    }

    public Trial trial() {
        return trial;
    }
}
