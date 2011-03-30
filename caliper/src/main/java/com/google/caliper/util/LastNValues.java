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

package com.google.caliper.util;

/**
 * Tracks the mean and variance of the last {@code n} values seen, where {@code n} is fixed at
 * construction.
 */
public class LastNValues {
  private double[] lastN;

  private int seen;
  private double sumOfLastN;
  private double sumOfSquaresOfLastN;

  public LastNValues(int n) {
    this.lastN = new double[n];
  }

  public void add(double newVal) {
    double oldVal = replace(seen++ % lastN.length, newVal);
    sumOfLastN += newVal - oldVal;
    sumOfSquaresOfLastN += squared(newVal) - squared(oldVal);
  }

  private double replace(int i, double d) {
    try {
      return lastN[i];
    } finally {
      lastN[i] = d;
    }
  }

  public boolean isFull() {
    return seen >= lastN.length;
  }

  public double mean() {
    return sumOfLastN / size();
  }

  public double variance() {
    return (sumOfSquaresOfLastN / size()) - squared(mean());
  }

  public double stddev() {
    return Math.sqrt(variance());
  }
  
  public double normalizedStddev() {
    return stddev() / mean();
  }

  private int size() {
    return Math.min(lastN.length, seen);
  }

  private static double squared(double d) {
    return d * d;
  }
}
