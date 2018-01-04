/*
 * Copyright (C) 2011 Google Inc.
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

package com.google.caliper.runner;

import com.google.auto.value.AutoValue;
import com.google.caliper.runner.Instrument.InstrumentedMethod;
import com.google.common.base.MoreObjects;
import com.google.common.collect.ImmutableSortedMap;
import java.util.Map;

/**
 * A single "premise" for making benchmark measurements: which class and method to invoke, which
 * target to use, which choices for user parameters and vmArguments to fill in and which instrument
 * to use to measure. A caliper run will compute all possible scenarios using {@link
 * ExperimentSelector}, and will run one or more trials of each.
 */
@AutoValue
abstract class Experiment {

  /** Creates a new {@link Experiment}. */
  static Experiment create(
      InstrumentedMethod instrumentedMethod, Map<String, String> userParameters, Target target) {
    return new AutoValue_Experiment(
        instrumentedMethod, ImmutableSortedMap.copyOf(userParameters), target);
  }

  /** Returns the instrumented method for this experiment. */
  abstract InstrumentedMethod instrumentedMethod();

  /** Returns the selection of user parameter values for this experiment. */
  abstract ImmutableSortedMap<String, String> userParameters();

  /** Returns the target this experiment is to be run on. */
  abstract Target target();

  @Override
  public String toString() {
    return MoreObjects.toStringHelper("")
        .add("instrument", instrumentedMethod().instrument())
        .add("benchmarkMethod", instrumentedMethod().benchmarkMethod().getName())
        .add("target", target().name())
        .add("parameters", userParameters())
        .toString();
  }
}
