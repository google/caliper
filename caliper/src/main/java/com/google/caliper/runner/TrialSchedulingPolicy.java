package com.google.caliper.runner;

/**
 * The scheduling policy for a particular trial.
 *
 * <p>TODO(lukes): Currently this is extremely simple.  Trials can be scheduled in parallel with
 * other trials or not.  In the future, this should use some kind of cost modeling.
 */
enum TrialSchedulingPolicy {
  PARALLEL,
  SERIAL;
}
