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
