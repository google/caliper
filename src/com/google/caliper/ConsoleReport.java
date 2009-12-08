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

package com.google.caliper;

import com.google.common.collect.*;

import java.util.*;

/**
 * Prints a report containing the tested values and the corresponding
 * measurements. Measurements are grouped by variable using indentation.
 * Alongside numeric values, quick-glance ascii art bar charts are printed.
 * Sample output:
 * <pre>
 *              benchmark                 d     ns logarithmic runtime
 * ConcatenationBenchmark 3.141592653589793   4397 ||||||||||||||||||||||||
 * ConcatenationBenchmark              -0.0    223 |||||||||||||||
 *     FormatterBenchmark 3.141592653589793  33999 ||||||||||||||||||||||||||||||
 *     FormatterBenchmark              -0.0  26399 |||||||||||||||||||||||||||||
 * </pre>
 */
final class ConsoleReport {

  private static final int bargraphWidth = 30;
  private static final String benchmarkKey = "benchmark";

  private final List<Parameter> parameters;
  private final Result result;
  private final List<Run> runs;

  private final double logMaxValue;
  private final int decimalDigits;
  private final double divideBy;
  private final String units;
  private final int measurementColumnLength;

  public ConsoleReport(Result result) {
    this.result = result;

    double minValue = Double.POSITIVE_INFINITY;
    double maxValue = 0;

    Multimap<String, String> nameToValues = LinkedHashMultimap.create();
    List<Parameter> parametersBuilder = new ArrayList<Parameter>();
    for (Map.Entry<Run, Double> entry : result.getMeasurements().entrySet()) {
      Run run = entry.getKey();
      double d = entry.getValue();

      minValue = minValue < d ? minValue : d;
      maxValue = maxValue > d ? maxValue : d;

      for (Map.Entry<String, String> parameter : run.getParameters().entrySet()) {
        String name = parameter.getKey();
        nameToValues.put(name, parameter.getValue());
      }

      nameToValues.put(benchmarkKey, run.getBenchmarkClass().getSimpleName());
    }

    for (Map.Entry<String, Collection<String>> entry : nameToValues.asMap().entrySet()) {
      Parameter parameter = new Parameter(entry.getKey(), entry.getValue());
      parametersBuilder.add(parameter);
    }

    for (Parameter parameter : parametersBuilder) {
      double[] measurements = new double[parameter.values.size()];
      for (Map.Entry<Run, Double> entry : result.getMeasurements().entrySet()) {
        Run run = entry.getKey();
        measurements[parameter.index(run)] += entry.getValue();
      }
      double total = 0;
      for (double value : measurements) {
        total += value;
      }
      double mean = total / measurements.length;
      double sum = 0;
      for (double value : measurements) {
        double distance = value - mean;
        sum += distance * distance;
      }
      parameter.stdDeviation = Math.sqrt(sum / measurements.length);
    }

    this.parameters = new StandardDeviationOrdering().reverse().sortedCopy(parametersBuilder);
    this.runs = new ByParametersOrdering().sortedCopy(result.getMeasurements().keySet());
    this.logMaxValue = Math.log(maxValue);

    int numDigitsInMin = (int) Math.ceil(Math.log10(minValue));
    if (numDigitsInMin > 9) {
      divideBy = 1000000000;
      decimalDigits = Math.max(0, 9 + 3 - numDigitsInMin);
      units = "s";
    } else if (numDigitsInMin > 6) {
      divideBy = 1000000;
      decimalDigits = Math.max(0, 6 + 3 - numDigitsInMin);
      units = "ms";
    } else if (numDigitsInMin > 3) {
      divideBy = 1000;
      decimalDigits = Math.max(0, 3 + 3 - numDigitsInMin);
      units = "us";
    } else {
      divideBy = 1;
      decimalDigits = 0;
      units = "ns";
    }
    measurementColumnLength = (int) Math.ceil(Math.log10(maxValue / divideBy)) + decimalDigits + 1;
  }

  /**
   * A parameter plus all of its values.
   */
  static class Parameter {
    final String name;
    final ImmutableList<String> values;
    final int maxLength;
    double stdDeviation;

    public Parameter(String name, Collection<String> values) {
      this.name = name;
      this.values = ImmutableList.copyOf(values);

      int maxLength = name.length();
      for (String value : values) {
        maxLength = Math.max(maxLength, value.length());
      }
      this.maxLength = maxLength;
    }

    String get(Run run) {
      return benchmarkKey.equals(name)
          ? run.getBenchmarkClass().getSimpleName()
          : run.getParameters().get(name);
    }

    int index(Run run) {
      return values.indexOf(get(run));
    }

    boolean isInteresting() {
      return values.size() > 1;
    }
  }

  /**
   * Orders the different parameters by their standard deviation. This results
   * in an appropriate grouping of output values.
   */
  static class StandardDeviationOrdering extends Ordering<Parameter> {
    public int compare(Parameter a, Parameter b) {
      return Double.compare(a.stdDeviation, b.stdDeviation);
    }
  }

  /**
   * Orders runs by the parameters.
   */
  class ByParametersOrdering extends Ordering<Run> {
    public int compare(Run a, Run b) {
      for (Parameter parameter : parameters) {
        int aValue = parameter.values.indexOf(parameter.get(a));
        int bValue = parameter.values.indexOf(parameter.get(b));
        int diff = aValue - bValue;
        if (diff != 0) {
          return diff;
        }
      }
      return 0;
    }
  }

  void displayResults() {
    printValues();
    System.out.println();
    printUninterestingParameters();
  }

  /**
   * Prints a table of values.
   */
  private void printValues() {
    for (Parameter parameter : parameters) {
      if (parameter.isInteresting()) {
        System.out.printf("%" + parameter.maxLength + "s ", parameter.name);
      }
    }
    System.out.printf("%" + measurementColumnLength + "s logarithmic runtime%n", units);

    String numbersFormat = "%" + measurementColumnLength + "." + decimalDigits + "f %s%n";
    for (Run run : runs) {
      for (Parameter parameter : parameters) {
        if (parameter.isInteresting()) {
          System.out.printf("%" + parameter.maxLength + "s ", parameter.get(run));
        }
      }
      double measurement = result.getMeasurements().get(run);
      System.out.printf(numbersFormat, measurement / divideBy, bargraph(measurement));
    }
  }

  /**
   * Prints parameters with only one unique value.
   */
  private void printUninterestingParameters() {
    for (Parameter parameter : parameters) {
      if (!parameter.isInteresting()) {
        System.out.println(parameter.name + ": " + Iterables.getOnlyElement(parameter.values));
      }
    }
  }

  /**
   * Returns a string containing a bar of proportional width to the specified
   * value.
   */
  private String bargraph(double value) {
    double logValue = Math.log(value);
    int numChars = (int) ((logValue / logMaxValue) * bargraphWidth);
    StringBuilder result = new StringBuilder(numChars);
    for (int i = 0; i < numChars; i++) {
      result.append("|");
    }
    return result.toString();
  }
}
