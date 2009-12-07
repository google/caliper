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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Prints a report containing the tested values and the correspdonding
 * measurements. Measurements are grouped by variable using indentation.
 * Alongside numeric values, quick-glance "bar charts" are printed. Sample
 * output:
 * <pre>
 * ConcatenationBenchmark
 *     -Infinity  350ns ||
 *          -0.0  389ns ||
 * FormatterBenchmark
 *     -Infinity 2918ns |||||||||||||||||
 *          -0.0 4257ns ||||||||||||||||||||||||
 * </pre>
 */
final class ConsoleReport {

  private final int bargraphWidth = 30;
  private final int wordWidth = 20;

  /**
   * Maps the list of values for each parameter to the corresponding result.
   */
  private final ImmutableMap<List<String>, Double> parametersToMeasurement;

  /**
   * Maps each parameter to its values.
   */
  private final ImmutableMultimap<String, String> parameters;

  private final ImmutableList<String> parameterNames;

  private final double maxValue;
  private final double divideBy;
  private final String units;
  private final int length;

  public ConsoleReport(Result result) {
    // figure out the full set of parameters
    Multimap<String, String> parametersBuilder = LinkedHashMultimap.create();
    for (Run run : result.getMeasurements().keySet()) {
      parametersBuilder.put("Benchmark", run.getBenchmarkClass().getSimpleName());
      for (Map.Entry<String, String> entry : run.getParameters().entrySet()) {
        parametersBuilder.put(entry.getKey(), entry.getValue());
      }
    }
    this.parameters = ImmutableMultimap.copyOf(parametersBuilder);
    this.parameterNames = ImmutableList.copyOf(parameters.keySet());

    ImmutableMap.Builder<List<String>, Double> parametersToMeasurementBuilder
        = ImmutableMap.builder();
    for (Map.Entry<Run, Double> entry : result.getMeasurements().entrySet()) {
      Run run = entry.getKey();
      ImmutableList.Builder<String> parametersAsKeyBuilder = ImmutableList.builder();
      // the first parameter is the benchmark class name
      parametersAsKeyBuilder.add(run.getBenchmarkClass().getSimpleName());
      for (int i = 1; i < parameterNames.size(); i++) {
        parametersAsKeyBuilder.add(run.getParameters().get(parameterNames.get(i)));
      }
      parametersToMeasurementBuilder.put(parametersAsKeyBuilder.build(), entry.getValue());
    }

    parametersToMeasurement = parametersToMeasurementBuilder.build();

    double minValue = Double.POSITIVE_INFINITY;
    double maxValue = 0;
    for (double d : result.getMeasurements().values()) {
      minValue = minValue < d ? minValue : d;
      maxValue = maxValue > d ? maxValue : d;
    }
    this.maxValue = maxValue;

    if (minValue > 1000000000) {
      divideBy = 1000000000;
      units = "s";
    } else if (minValue > 1000000) {
      divideBy = 1000000;
      units = "ms";
    } else if (minValue > 1000) {
      divideBy = 1000;
      units = "us";
    } else {
      divideBy = 1;
      units = "ns";
    }
    length = (int) Math.ceil(Math.log10(maxValue / divideBy));
  }

  void displayResults() {
    List<String> key = new ArrayList<String>();
    displayRecursively(key, 0);
  }

  private void displayRecursively(List<String> key, int indentationLevel) {
    String parameterName = parameterNames.get(key.size());
    int index = key.size();

    ImmutableCollection<String> parameterValues = parameters.get(parameterName);
    String formatString = "%" + wordWidth + "s %" + length + ".0f%s %s%n";

    for (String parameterValue : parameterValues) {
      key.add(index, parameterValue);
      if (key.size() == parameterNames.size()) {
        indent(indentationLevel);
        Double measurement = parametersToMeasurement.get(key);
        double unitsPerTrial = measurement / divideBy;
        System.out.printf(formatString, parameterValue, unitsPerTrial, units, bargraph(measurement));

      } else {
        indent(indentationLevel);
        System.out.println(parameterValue);
        displayRecursively(key, indentationLevel + 1);
      }
      key.remove(index);
    }
  }

  private void indent(int indentationLevel) {
    for (int i = 0; i < indentationLevel; i++) {
      System.out.print("  ");
    }
  }

  /**
   * Returns a string containing a bar of proportional width to the specified
   * value.
   */
  private String bargraph(double value) {
    int numChars = (int) ((value / maxValue) * bargraphWidth);
    StringBuilder result = new StringBuilder(numChars);
    for (int i = 0; i < numChars; i++) {
      result.append("|");
    }
    return result.toString();
  }
}
