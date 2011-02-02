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

import com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import com.google.common.collect.LinkedHashMultimap;
import com.google.common.collect.Multimap;
import com.google.common.collect.Ordering;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

/**
 * Prints a report containing the tested values and the corresponding
 * measurements. Measurements are grouped by variable using indentation.
 * Alongside numeric values, quick-glance ascii art bar charts are printed.
 * Sample output (this may not represent the exact form that is produced):
 * <pre>
 *              benchmark          d     ns linear runtime
 * ConcatenationBenchmark 3.14159265   4397 ========================
 * ConcatenationBenchmark       -0.0    223 ===============
 *     FormatterBenchmark 3.14159265  33999 ==============================
 *     FormatterBenchmark       -0.0  26399 =============================
 * </pre>
 */
final class ConsoleReport {

  private static final int barGraphWidth = 30;

  private static final int UNITS_FOR_SCORE_100 = 1;
  private static final int UNITS_FOR_SCORE_10 = 1000000000; // 1 s

  private static final LinearTranslation scoreTranslation =
      new LinearTranslation(Math.log(UNITS_FOR_SCORE_10), 10,
                            Math.log(UNITS_FOR_SCORE_100), 100);

  public static final Ordering<Entry<String, Integer>> UNIT_ORDERING =
      new Ordering<Entry<String, Integer>>() {
        @Override public int compare(Entry<String, Integer> a, Entry<String, Integer> b) {
          return a.getValue().compareTo(b.getValue());
        }
      };

  private final List<Variable> variables;
  private final Run run;
  private final List<Scenario> scenarios;

  private final List<MeasurementType> orderedMeasurementTypes;
  private final MeasurementType type;
  private final double maxValue;
  private final double logMinValue;
  private final double logMaxValue;
  private final EnumMap<MeasurementType, Integer> decimalDigitsMap =
      new EnumMap<MeasurementType, Integer>(MeasurementType.class);
  private final EnumMap<MeasurementType, Double> divideByMap =
      new EnumMap<MeasurementType, Double>(MeasurementType.class);
  private final EnumMap<MeasurementType, String> unitMap =
      new EnumMap<MeasurementType, String>(MeasurementType.class);
  private final EnumMap<MeasurementType, Integer> measurementColumnLengthMap =
      new EnumMap<MeasurementType, Integer>(MeasurementType.class);
  private boolean printScore;

  ConsoleReport(Run run, Arguments arguments) {
    this.run = run;
    unitMap.put(MeasurementType.TIME, arguments.getTimeUnit());
    unitMap.put(MeasurementType.INSTANCE, arguments.getInstanceUnit());
    unitMap.put(MeasurementType.MEMORY, arguments.getMemoryUnit());

    if (arguments.getMeasureMemory()) {
      orderedMeasurementTypes = Arrays.asList(
          MeasurementType.TIME, MeasurementType.INSTANCE, MeasurementType.MEMORY);
    } else {
      orderedMeasurementTypes = Arrays.asList(MeasurementType.TIME);
    }

    if (arguments.getPrimaryMeasurementType() != null) {
      this.type = arguments.getPrimaryMeasurementType();
    } else {
      this.type = MeasurementType.TIME;
    }

    double min = Double.POSITIVE_INFINITY;
    double max = 0;

    Multimap<String, String> nameToValues = LinkedHashMultimap.create();
    List<Variable> variablesBuilder = new ArrayList<Variable>();
    for (Entry<Scenario, ScenarioResult> entry : this.run.getMeasurements().entrySet()) {
      Scenario scenario = entry.getKey();
      double d = entry.getValue().getMeasurementSet(type).medianUnits();

      min = Math.min(min, d);
      max = Math.max(max, d);

      for (Entry<String, String> variable : scenario.getVariables().entrySet()) {
        String name = variable.getKey();
        nameToValues.put(name, variable.getValue());
      }
    }

    for (Entry<String, Collection<String>> entry : nameToValues.asMap().entrySet()) {
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
    for (ScenarioResult measurement : this.run.getMeasurements().values()) {
      sumOfAllMeasurements += measurement.getMeasurementSet(type).medianUnits();
    }
    for (Variable variable : variablesBuilder) {
      int numValues = variable.values.size();
      double[] sumForValue = new double[numValues];
      for (Entry<Scenario, ScenarioResult> entry
          : this.run.getMeasurements().entrySet()) {
        Scenario scenario = entry.getKey();
        sumForValue[variable.index(scenario)] +=
            entry.getValue().getMeasurementSet(type).medianUnits();
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
    this.scenarios = new ByVariablesOrdering().sortedCopy(this.run.getMeasurements().keySet());
    this.maxValue = max;
    this.logMinValue = Math.log(min);
    this.logMaxValue = Math.log(max);

    EnumMap<MeasurementType, Integer> digitsBeforeDecimalMap =
      new EnumMap<MeasurementType, Integer>(MeasurementType.class);
    EnumMap<MeasurementType, Integer> decimalPointMap =
      new EnumMap<MeasurementType, Integer>(MeasurementType.class);
    for (MeasurementType measurementType : orderedMeasurementTypes) {
      double maxForType = 0;
      double minForType = Double.POSITIVE_INFINITY;
      for (Entry<Scenario, ScenarioResult> entry : this.run.getMeasurements().entrySet()) {
        double d = entry.getValue().getMeasurementSet(measurementType).medianUnits();
        minForType = Math.min(minForType, d);
        maxForType = Math.max(maxForType, d);
      }

      unitMap.put(measurementType,
          getUnit(unitMap.get(measurementType), measurementType, minForType));

      divideByMap.put(measurementType,
          (double) getUnits(measurementType).get(unitMap.get(measurementType)));

      int numDigitsInMin = ceil(Math.log10(minForType));
      decimalDigitsMap.put(measurementType,
          ceil(Math.max(0, ceil(Math.log10(divideByMap.get(measurementType))) + 3 - numDigitsInMin)));

      digitsBeforeDecimalMap.put(measurementType,
          Math.max(1, ceil(Math.log10(maxForType / divideByMap.get(measurementType)))));

      decimalPointMap.put(measurementType, decimalDigitsMap.get(measurementType) > 0 ? 1 : 0);

      measurementColumnLengthMap.put(measurementType, Math.max(maxForType > 0
          ?  digitsBeforeDecimalMap.get(measurementType) + decimalPointMap.get(measurementType)
              + decimalDigitsMap.get(measurementType)
          : 1, unitMap.get(measurementType).trim().length()));
    }

    this.printScore = arguments.printScore();
  }

  private String getUnit(String userSuppliedUnit, MeasurementType measurementType, double min) {
    Map<String, Integer> units = getUnits(measurementType);

    if (userSuppliedUnit == null) {
      List<Entry<String, Integer>> entries = UNIT_ORDERING.reverse().sortedCopy(units.entrySet());
      for (Entry<String, Integer> entry : entries) {
        if (min / entry.getValue() >= 1) {
          return entry.getKey();
        }
      }
      // if no unit works, just use the smallest available unit.
      return entries.get(entries.size() - 1).getKey();
    }

    if (!units.keySet().contains(userSuppliedUnit)) {
      throw new RuntimeException("\"" + unitMap.get(measurementType) + "\" is not a valid unit.");
    }
    return userSuppliedUnit;
  }

  private Map<String, Integer> getUnits(MeasurementType measurementType) {
    Map<String, Integer> units = null;
    for (Entry<Scenario, ScenarioResult> entry : run.getMeasurements().entrySet()) {
      if (units == null) {
        units = entry.getValue().getMeasurementSet(measurementType).getUnitNames();
      } else {
        if (!units.equals(entry.getValue().getMeasurementSet(measurementType).getUnitNames())) {
          throw new RuntimeException("measurement sets for run contain multiple, incompatible unit"
              + " sets.");
        }
      }
    }
    if (units == null) {
      throw new RuntimeException("run has no measurements.");
    }
    if (units.isEmpty()) {
      throw new RuntimeException("no units specified.");
    }
    return units;
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
    printCharCounts();
  }

  private void printCharCounts() {
    int systemOutCharCount = 0;
    int systemErrCharCount = 0;
    for (ScenarioResult scenarioResult : run.getMeasurements().values()) {
      for (MeasurementType measurementType : MeasurementType.values()) {
        MeasurementSet measurementSet = scenarioResult.getMeasurementSet(measurementType);
        if (measurementSet != null) {
          systemOutCharCount += measurementSet.getSystemOutCharCount();
          systemErrCharCount += measurementSet.getSystemErrCharCount();
        }
      }
    }
    if (systemOutCharCount > 0 || systemErrCharCount > 0) {
      System.out.println();
      System.out.println("Note: benchmarks printed " + systemOutCharCount
          + " characters to System.out and " + systemErrCharCount + " characters to System.err."
          + " Use --debug to see this output.");
    }
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
    // doesn't make sense to show graphs at all for 1
    // scenario, since it leads to vacuous graphs.
    boolean showGraphs = scenarios.size() > 1;

    EnumMap<MeasurementType, String> numbersFormatMap =
        new EnumMap<MeasurementType, String>(MeasurementType.class);
    for (MeasurementType measurementType : orderedMeasurementTypes) {
      if (measurementType != type) {
        System.out.printf("%" + measurementColumnLengthMap.get(measurementType) + "s ",
            unitMap.get(measurementType).trim());
      }

      numbersFormatMap.put(measurementType,
          "%" + measurementColumnLengthMap.get(measurementType)
              + "." + decimalDigitsMap.get(measurementType) + "f"
              + (type == measurementType ? "" : " "));
    }

    System.out.printf("%" + measurementColumnLengthMap.get(type) + "s", unitMap.get(type).trim());
    if (showGraphs) {
      System.out.print(" linear runtime");
    }
    System.out.println();

    double sumOfLogs = 0.0;

    for (Scenario scenario : scenarios) {
      for (Variable variable : variables) {
        if (variable.isInteresting()) {
          System.out.printf("%" + variable.maxLength + "s ", variable.get(scenario));
        }
      }
      ScenarioResult measurement = run.getMeasurements().get(scenario);
      sumOfLogs += Math.log(measurement.getMeasurementSet(type).medianUnits());

      for (MeasurementType measurementType : orderedMeasurementTypes) {
        if (measurementType != type) {
          System.out.printf(numbersFormatMap.get(measurementType),
              measurement.getMeasurementSet(measurementType).medianUnits() / divideByMap.get(measurementType));
        }
      }

      System.out.printf(numbersFormatMap.get(type),
          measurement.getMeasurementSet(type).medianUnits() / divideByMap.get(type));
      if (showGraphs) {
        System.out.printf(" %s", barGraph(measurement.getMeasurementSet(type).medianUnits()));
      }
      System.out.println();
    }

    if (printScore) {
      // arithmetic mean of logs, aka log of geometric mean
      double meanLogUnits = sumOfLogs / scenarios.size();
      System.out.format("%nScore: %.3f%n", scoreTranslation.translate(meanLogUnits));
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
  private String barGraph(double value) {
    int graphLength = floor(value / maxValue * barGraphWidth);
    graphLength = Math.max(1, graphLength);
    graphLength = Math.min(barGraphWidth, graphLength);
    return Strings.repeat("=", graphLength);
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
