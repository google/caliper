/*
 * Copyright (C) 2010 Google Inc.
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

package com.google.caliper;

import java.io.Serializable;
import java.util.Arrays;

/**
 * A collection of measurements of the same scenario.
 */
public final class MeasurementSet
    implements Serializable /* for GWT Serialization */ {

  private /*final*/ double[] measurements;

  public MeasurementSet(double... measurements) {
    this.measurements = cloneDoubleArray(measurements);
    Arrays.sort(this.measurements);
  }

  /**
   * Returns the measurements in sorted order.
   */
  public double[] getMeasurements() {
    return cloneDoubleArray(measurements);
  }

  /**
   * Returns the median measurement.
   */
  public double getMedian() {
    // TODO: average middle two if the set is even-sized
    return measurements[measurements.length / 2];
  }

  /**
   * Returns the minimum measurement.
   */
  public double getMin() {
    return measurements[0];
  }

  /**
   * Returns the maximum measurement.
   */
  public double getMax() {
    return measurements[measurements.length - 1];
  }

  /**
   * Creates a MeasurementSet from a string of space-separated measurements.
   */
  public static MeasurementSet valueOf(String value) {
    try {
      String[] measurementStrings = value.split("\\s+");
      double[] measurements = new double[measurementStrings.length];
      int i = 0;
      for (String s : measurementStrings) {
        measurements[i++] = Double.valueOf(s);
      }
      return new MeasurementSet(measurements);
    } catch (NumberFormatException ignore) {
      throw new IllegalArgumentException("Not a measurement set: " + value);
    }
  }

  /**
   * Returns a string of space-separated measurements.
   */
  @Override public String toString() {
    StringBuilder result = new StringBuilder();
    for (double measurement : measurements) {
      if (result.length() > 0) {
        result.append(" ");
      }
      result.append(measurement);
    }
    return result.toString();
  }

  /**
   * Returns a copy of the input. GWT doesn't support cloning arrays, so we need
   * to do it by hand.
   */
  private double[] cloneDoubleArray(double[] input) {
    double[] result = new double[input.length];
    for (int i = 0; i < input.length; i++) {
      result[i] = input[i];
    }
    return result;
  }

  private MeasurementSet() {} // for GWT Serialization
}
