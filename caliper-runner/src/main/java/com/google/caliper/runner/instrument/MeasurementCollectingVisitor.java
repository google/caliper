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

package com.google.caliper.runner.instrument;

import com.google.caliper.bridge.LogMessageVisitor;
import com.google.caliper.model.Measurement;
import com.google.common.collect.ImmutableList;

/** {@link LogMessageVisitor} for collecting benchmark measurements. */
public interface MeasurementCollectingVisitor extends LogMessageVisitor {

  /** Returns whether or not this visitor is done collecting measurements. */
  boolean isDoneCollecting();

  /** Returns whether or not warmup is complete. */
  boolean isWarmupComplete();

  /** Returns the collected measurements. */
  ImmutableList<Measurement> getMeasurements();

  /**
   * Returns all the messages created while collecting measurments.
   *
   * <p>A message is some piece of user visible data that should be displayed to the user along with
   * the trial results.
   */
  // TODO(lukes): should we model these as anything more than strings? These messages already
  // have a concept of 'level' based on the prefix.
  ImmutableList<String> getMessages();
}
