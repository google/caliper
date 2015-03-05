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

import com.google.caliper.model.Trial;
import com.google.common.collect.ImmutableList;

/**
 * A simple tuple of the data
 */
final class TrialResult {
  private final Trial trial;
  private final Experiment experiment;
  private final ImmutableList<String> trialMessages;

  TrialResult(Trial trial, Experiment experiment, ImmutableList<String> trialMessages) {
    this.trial = trial;
    this.experiment = experiment;
    this.trialMessages = trialMessages;
  }
  
  Experiment getExperiment() {
    return experiment;
  }

  Trial getTrial() {
    return trial;
  }

  ImmutableList<String> getTrialMessages() {
    return trialMessages;
  }
}
