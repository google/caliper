package com.google.caliper.runner;


import java.util.concurrent.Callable;

import javax.inject.Inject;

/**
 * A ScheduledTrial is a simple pair of a {@link TrialRunLoop} and a 
 * {@link TrialSchedulingPolicy}.
 */
@TrialScoped final class ScheduledTrial {
  private final TrialRunLoop runLoop;
  private final Experiment experiment;
  private final TrialSchedulingPolicy policy;

  @Inject ScheduledTrial(Experiment experiment, TrialRunLoop runLoop, 
      TrialSchedulingPolicy policy) {
    this.runLoop = runLoop;
    this.experiment = experiment;
    this.policy = policy;
  }
  
  TrialSchedulingPolicy policy() {
    return policy;
  }
  
  Experiment experiment() {
    return experiment;
  }
  
  Callable<TrialResult> trialTask() {
    return runLoop;
  }
}
