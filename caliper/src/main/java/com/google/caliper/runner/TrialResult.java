// Copyright 2011 Google Inc. All Rights Reserved.

package com.google.caliper.runner;

import com.google.caliper.model.Measurement;
import com.google.common.collect.ImmutableList;

import java.util.Collection;
import java.util.List;

/**
 * A simple data object that packages a {@link Scenario} with the output of a single trial.
 */
class TrialResult {
  private final Scenario scenario;
  private final ImmutableList<Measurement> measurements;
  private final ImmutableList<String> messages;
  private final ImmutableList<String> vmCommandLine;

  TrialResult(Scenario scenario, Collection<Measurement> measurements, List<String> messages,
      List<String> vmCommandLine) {

    this.scenario = scenario;
    this.measurements = ImmutableList.copyOf(measurements);
    this.messages = ImmutableList.copyOf(messages);
    this.vmCommandLine = ImmutableList.copyOf(vmCommandLine);
  }

  public Scenario getScenario() {
    return scenario;
  }

  public ImmutableList<Measurement> getMeasurements() {
    return measurements;
  }

  public ImmutableList<String> getMessages() {
    return messages;
  }

  public ImmutableList<String> getVmCommandLine() {
    return vmCommandLine;
  }
}
