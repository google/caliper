/**
 * Copyright (C) 2009 Google Inc.
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

package com.google.caliper.cloud.client;

import com.google.caliper.MeasurementSet;

public class Value {

  private final String name;
  private boolean shown = true;
  private double max = 0.0;
  private double min = Double.POSITIVE_INFINITY;
  private double referencePoint = Double.POSITIVE_INFINITY;

  public Value(String name) {
    this.name = name;
  }

  public final String getName() {
    return name;
  }

  public String getLabel() {
    return name;
  }

  /**
   * Populates summary statistics of this value from a measurement taken with
   * this value.
   */
  public final void index(MeasurementSet measurementSet, boolean useNanos) {
    double maxUnits = useNanos ? measurementSet.maxRaw() : measurementSet.maxUnits();
    if (max < maxUnits) {
      max = maxUnits;
    }
    double minUnits = useNanos ? measurementSet.minRaw() : measurementSet.minUnits();
    if (min > minUnits) {
      min = minUnits;
      referencePoint = minUnits;
    }
  }

  public final void resetIndex() {
    max = 0.0;
    min = Double.POSITIVE_INFINITY;
    referencePoint = Double.POSITIVE_INFINITY;
  }

  /**
   * Returns the maximum measurement taken with this value.
   */
  public final double getMax() {
    return max;
  }

  /**
   * Returns the minimum measurement taken with this value.
   */
  public final double getMin() {
    return min;
  }

  /**
   * The reference point is a value assigned as "100%". Other variables are
   * computed as a multiple of this, like 80% or 125%.
   */
  public final double getReferencePoint() {
    return referencePoint;
  }

  public void setReferencePoint(double referencePoint) {
    this.referencePoint = referencePoint;
  }

  public final boolean isShown() {
    return shown;
  }

  public final void setShown(boolean shown) {
    this.shown = shown;
  }

  @Override public String toString() {
    return name;
  }
}
