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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * A collection of measurements of the same scenario.
 */
@SuppressWarnings("serial")
public final class MeasurementSet
    implements Serializable /* for GWT Serialization */ {

  private /*final*/ List<Measurement> measurements;
  /**
   * Mapping of user-defined units to relative sizes.
   */
  private /*final*/ Map<String, Integer> unitNames;

  public MeasurementSet(Measurement... measurements) {
    this(getUnitNamesFromMeasurements(measurements), Arrays.asList(measurements));
  }

  private static Map<String, Integer> getUnitNamesFromMeasurements(Measurement... measurements) {
    Map<String, Integer> unitNameToAssign = null;
    for (Measurement measurement : measurements) {
      if (unitNameToAssign == null) {
        unitNameToAssign = new HashMap<String, Integer>(measurement.getUnitNames());
      } else if (!unitNameToAssign.equals(measurement.getUnitNames())) {
        throw new IllegalArgumentException("incompatible unit names: " + unitNameToAssign + " and "
            + measurement.getUnitNames());
      }
    }
    return unitNameToAssign;
  }

  /**
   * Constructor to use directly from plusMeasurement. Skips some excessive checking and takes a
   * list directly.
   */
  private MeasurementSet(Map<String, Integer> unitNames, List<Measurement> measurements) {
    this.unitNames = unitNames;
    this.measurements = measurements;
  }

  public Map<String, Integer> getUnitNames() {
    return new HashMap<String, Integer>(unitNames);
  }

  public List<Measurement> getMeasurements() {
    return new ArrayList<Measurement>(measurements);
  }

  public int size() {
    return measurements.size();
  }

  public List<Double> getMeasurementNanos() {
    List<Double> measurementNanos = new ArrayList<Double>();
    for (Measurement measurement : measurements) {
      measurementNanos.add(measurement.getNanosPerRep());
    }
    return measurementNanos;
  }

  public List<Double> getMeasurementUnits() {
    List<Double> measurementUnits = new ArrayList<Double>();
    for (Measurement measurement : measurements) {
      measurementUnits.add(measurement.getUnitsPerRep());
    }
    return measurementUnits;
  }

  /**
   * Returns the median measurement, with respect to nanoseconds.
   */
  public double medianNanos() {
    return median(getMeasurementNanos());
  }

  /**
   * Returns the median measurement, with respect to user-defined units.
   */
  public double medianUnits() {
    return median(getMeasurementUnits());
  }

  private double median(List<Double> doubles) {
    Collections.sort(doubles);
    int n = doubles.size();
    return (n % 2 == 0)
        ? (doubles.get(n / 2 - 1) + doubles.get(n / 2)) / 2
        : doubles.get(n / 2);
  }

  /**
   * Returns the average measurement with respect to nanoseconds..
   */
  public double meanNanos() {
    return mean(getMeasurementNanos());
  }

  /**
   * Returns the average measurement with respect to user-defined units.
   */
  public double meanUnits() {
    return mean(getMeasurementUnits());
  }

  private double mean(List<Double> doubles) {
    double sum = 0;
    for (double d : doubles) {
      sum += d;
    }
    return sum / doubles.size();
  }

  public double standardDeviationNanos() {
    return standardDeviation(getMeasurementNanos());
  }

  public double standardDeviationUnits() {
    return standardDeviation(getMeasurementUnits());
  }

  /**
   * Returns the standard deviation of the measurements.
   */
  private double standardDeviation(List<Double> doubles) {
    double mean = mean(doubles);
    double sumOfSquares = 0;
    for (double d : doubles) {
      double delta = (d - mean);
      sumOfSquares += (delta * delta);
    }
    return Math.sqrt(sumOfSquares / (doubles.size() - 1));
  }

  public double minNanos() {
    return min(getMeasurementNanos());
  }

  public double minUnits() {
    return min(getMeasurementUnits());
  }

  /**
   * Returns the minimum measurement.
   */
  private double min(List<Double> doubles) {
    Collections.sort(doubles);
    return doubles.get(0);
  }

  public double maxNanos() {
    return max(getMeasurementNanos());
  }

  public double maxUnits() {
    return max(getMeasurementUnits());
  }

  /**
   * Returns the maximum measurement.
   */
  private double max(List<Double> doubles) {
    Collections.sort(doubles, Collections.reverseOrder());
    return doubles.get(0);
  }

  /**
   * Returns a new measurement set that contains the measurements in this set
   * plus the given additional measurement.
   */
  public MeasurementSet plusMeasurement(Measurement measurement) {
    // verify that this Measurement is compatible with this MeasurementSet
    if (unitNames != null && !unitNames.equals(measurement.getUnitNames())) {
      throw new IllegalArgumentException("new measurement incompatible with units of measurement "
          + "set. Expected " + unitNames + " but got " + measurement.getUnitNames());
    }

    List<Measurement> resultMeasurements = new ArrayList<Measurement>(measurements);
    resultMeasurements.add(measurement);
    Map<String, Integer> newUnitNames = unitNames == null ? measurement.getUnitNames() : unitNames;
    return new MeasurementSet(newUnitNames, resultMeasurements);
  }

  private MeasurementSet() {} // for GWT Serialization
}
