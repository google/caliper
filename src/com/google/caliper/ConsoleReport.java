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

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import com.google.common.collect.LinkedHashMultimap;
import com.google.common.collect.Multimap;
import com.google.common.collect.Ordering;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

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

  private final List<Variable> variables;
  private final Run run;
  private final List<Scenario> scenarios;

  private final double maxValue;
  private final double logMaxValue;
  private final int decimalDigits;
  private final double divideBy;
  private final String units;
  private final int measurementColumnLength;

  ConsoleReport(Run run) {
    this.run = run;

    double min = Double.POSITIVE_INFINITY;
    double max = 0;

    Multimap<String, String> nameToValues = LinkedHashMultimap.create();
    List<Variable> variablesBuilder = new ArrayList<Variable>();
    for (Map.Entry<Scenario, MeasurementSet> entry : run.getMeasurements().entrySet()) {
      Scenario scenario = entry.getKey();
      double d = entry.getValue().median();

      min = Math.min(min, d);
      max = Math.max(max, d);

      for (Map.Entry<String, String> variable : scenario.getVariables().entrySet()) {
        String name = variable.getKey();
        nameToValues.put(name, variable.getValue());
      }
    }

    for (Map.Entry<String, Collection<String>> entry : nameToValues.asMap().entrySet()) {
      Variable variable = new Variable(entry.getKey(), entry.getValue());
      variablesBuilder.add(variable);
    }

    /*
     * Figure out how much influence each variable has on the measured value.
     * We sum the measurements taken with each value of each variable. For
     * variable that have influence on the measurement, the sums will differ
     * by value. If the variable has little influence, the sums will be similar
     * to one another and close to the overall average. We take the standard
     * deviation across each variable's collection of sums. Higher standard
     * deviation implies higher influence on the measured result.
     */
    double sumOfAllMeasurements = 0;
    for (MeasurementSet measurement : run.getMeasurements().values()) {
      sumOfAllMeasurements += measurement.median();
    }
    for (Variable variable : variablesBuilder) {
      int numValues = variable.values.size();
      double[] sumForValue = new double[numValues];
      for (Map.Entry<Scenario, MeasurementSet> entry : run.getMeasurements().entrySet()) {
        Scenario scenario = entry.getKey();
        sumForValue[variable.index(scenario)] += entry.getValue().median();
      }
      double mean = sumOfAllMeasurements / sumForValue.length;
      double stdDeviationSquared = 0;
      for (double value : sumForValue) {
        double distance = value - mean;
        stdDeviationSquared += distance * distance;
      }
      variable.stdDeviation = Math.sqrt(stdDeviationSquared / numValues);
    }

    this.variables = new StandardDeviationOrdering().reverse().sortedCopy(variablesBuilder);
    this.scenarios = new ByVariablesOrdering().sortedCopy(run.getMeasurements().keySet());
    this.maxValue = max;
    this.logMaxValue = Math.log(max);

    int numDigitsInMin = ceil(Math.log10(min));
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
    measurementColumnLength = max > 0
        ? ceil(Math.log10(max / divideBy)) + decimalDigits + 1
        : 1;
  }

  /**
   * A variable and the set of values to which it has been assigned.
   */
  private static class Variable {
    final String name;
    final ImmutableList<String> values;
    final int maxLength;
    double stdDeviation;

    Variable(String name, Collection<String> values) {
      this.name = name;
      this.values = ImmutableList.copyOf(values);

      int maxLen = name.length();
      for (String value : values) {
        maxLen = Math.max(maxLen, value.length());
      }
      this.maxLength = maxLen;
    }

    String get(Scenario scenario) {
      return scenario.getVariables().get(name);
    }

    int index(Scenario scenario) {
      return values.indexOf(get(scenario));
    }

    boolean isInteresting() {
      return values.size() > 1;
    }
  }

  /**
   * Orders the different variables by their standard deviation. This results
   * in an appropriate grouping of output values.
   */
  private static class StandardDeviationOrdering extends Ordering<Variable> {
    public int compare(Variable a, Variable b) {
      return Double.compare(a.stdDeviation, b.stdDeviation);
    }
  }

  /**
   * Orders scenarios by the variables.
   */
  private class ByVariablesOrdering extends Ordering<Scenario> {
    public int compare(Scenario a, Scenario b) {
      for (Variable variable : variables) {
        int aValue = variable.values.indexOf(variable.get(a));
        int bValue = variable.values.indexOf(variable.get(b));
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
    printUninterestingVariables();
  }

  /**
   * Prints a table of values.
   */
  private void printValues() {
    // header
    for (Variable variable : variables) {
      if (variable.isInteresting()) {
        System.out.printf("%" + variable.maxLength + "s ", variable.name);
      }
    }
    System.out.printf("%" + measurementColumnLength + "s logarithmic runtime%n", units);

    // rows
    String numbersFormat = "%" + measurementColumnLength + "." + decimalDigits + "f %s%n";
    for (Scenario scenario : scenarios) {
      for (Variable variable : variables) {
        if (variable.isInteresting()) {
          System.out.printf("%" + variable.maxLength + "s ", variable.get(scenario));
        }
      }
      double measurement = run.getMeasurements().get(scenario).median();
      System.out.printf(numbersFormat, measurement / divideBy, bargraph(measurement));
    }
  }

  /**
   * Prints variables with only one unique value.
   */
  private void printUninterestingVariables() {
    for (Variable variable : variables) {
      if (!variable.isInteresting()) {
        System.out.println(variable.name + ": " + Iterables.getOnlyElement(variable.values));
      }
    }
  }

  /**
   * Returns a string containing a bar of proportional width to the specified
   * value.
   */
  private String bargraph(double value) {
    int numLinearChars = floor(value / maxValue * bargraphWidth);
    double logValue = Math.log(value);
    int numChars = floor(logValue / logMaxValue * bargraphWidth);
    StringBuilder sb = new StringBuilder(numChars);
    for (int i = 0; i < numLinearChars; i++) {
      sb.append("X");
    }

    for (int i = numLinearChars; i < numChars; i++) {
      sb.append("|");
    }
    return sb.toString();
  }

  @SuppressWarnings("NumericCastThatLosesPrecision")
  private static int floor(double d) {
    return (int) d;
  }

  @SuppressWarnings("NumericCastThatLosesPrecision")
  private static int ceil(double d) {
    return (int) Math.ceil(d);
  }
}
