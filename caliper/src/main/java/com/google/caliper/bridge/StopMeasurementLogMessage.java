/*
 * Copyright (C) 2012 Google Inc.
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

package com.google.caliper.bridge;

import com.google.caliper.model.Measurement;
import com.google.common.base.Objects;
import com.google.common.collect.ImmutableList;

import java.io.Serializable;

/**
 * A message signaling that the timing interval has ended in the worker.
 */
public class StopMeasurementLogMessage extends LogMessage implements Serializable {
  private static final long serialVersionUID = 1L;

  private final ImmutableList<Measurement> measurements;

  public StopMeasurementLogMessage(Iterable<Measurement> measurements) {
    this.measurements = ImmutableList.copyOf(measurements);
  }

  public ImmutableList<Measurement> measurements() {
    return measurements;
  }

  @Override public void accept(LogMessageVisitor visitor) {
    visitor.visit(this);
  }

  @Override public int hashCode() {
    return Objects.hashCode(measurements);
  }

  @Override
  public boolean equals(Object obj) {
    if (obj == this) {
      return true;
    } else if (obj instanceof StopMeasurementLogMessage) {
      StopMeasurementLogMessage that = (StopMeasurementLogMessage) obj;
      return this.measurements.equals(that.measurements);
    } else {
      return false;
    }
  }
}
