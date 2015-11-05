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

import com.google.caliper.LinearTranslation;
import com.google.caliper.Measurement;
import com.google.caliper.MeasurementSet;
import com.google.caliper.MeasurementType;
import com.google.caliper.Run;
import com.google.caliper.Scenario;
import com.google.caliper.ScenarioResult;
import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.logical.shared.ValueChangeEvent;
import com.google.gwt.event.logical.shared.ValueChangeHandler;
import com.google.gwt.http.client.URL;
import com.google.gwt.i18n.client.DateTimeFormat;
import com.google.gwt.i18n.client.NumberFormat;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Anchor;
import com.google.gwt.user.client.ui.CheckBox;
import com.google.gwt.user.client.ui.DialogBox;
import com.google.gwt.user.client.ui.FlexTable;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.Grid;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.InlineLabel;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.Panel;
import com.google.gwt.user.client.ui.RootPanel;
import com.google.gwt.user.client.ui.Widget;
import com.google.gwt.widgetideas.graphics.client.Color;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * The core data viewer user interface. The variables are divided into three
 * categories:
 * <ul>
 *   <li>Fixed: variables with only one value
 *   <li>R Variables: variables whose values vary in the rows of the table
 *   <li>C Variable: a variable whose values make up the columns of the table
 * <ul>
 */
public final class BenchmarkDataViewer {
  private static final AsyncCallback<Void> NO_OP_CALLBACK = new AsyncCallback<Void>() {
    public void onFailure(Throwable unused) {}
    public void onSuccess(Void unused) {}
  };

  /** bar chart dimension in pixels */
  private static final int MAX_BAR_WIDTH = 200;
  private static final int MAX_BAR_HEIGHT = 18;

  private static final int DIGITS_OF_PRECISION = 3;

  private static final Map<MeasurementType, Map<String, Integer>> DEFAULT_UNITS =
      new HashMap<MeasurementType, Map<String, Integer>>();
  static {
    Map<String, Integer> timeUnits = new HashMap<String, Integer>();
    timeUnits.put("ns", 1);
    timeUnits.put("us", 1000);
    timeUnits.put("ms", 1000000);
    timeUnits.put("s",  1000000000);
    DEFAULT_UNITS.put(MeasurementType.TIME, timeUnits);
    Map<String, Integer> instanceUnits = new HashMap<String, Integer>();
    instanceUnits.put(" instances", 1);
    instanceUnits.put("K instances", 1000);
    instanceUnits.put("M instances", 1000000);
    instanceUnits.put("B instances",  1000000000);
    DEFAULT_UNITS.put(MeasurementType.INSTANCE, instanceUnits);
    Map<String, Integer> memoryUnits = new HashMap<String, Integer>();
    memoryUnits.put("B", 1);
    memoryUnits.put("KB", 1024);
    memoryUnits.put("MB", 1048576);
    memoryUnits.put("GB",  1073741824);
    DEFAULT_UNITS.put(MeasurementType.MEMORY, memoryUnits);
  }

  private final boolean editable;
  private final String benchmarkOwner;
  private final String benchmarkName;
  private final long snapshotId;
  private final RootPanel snapshotDisclaimerDiv;
  private final RootPanel resultsDiv;
  private final RootPanel fixedVariablesDiv;
  private final RootPanel runsDiv;
  private final SnapshotsTableDisplay snapshotsTableDisplay;
  private final EnvironmentsTableDisplay environmentsTableDisplay;
  private final EnvironmentsIndex environmentsIndex;
  private final List<RunMeta> runMetas = new ArrayList<RunMeta>();

  /** the full set of application variables */
  private final Map<String, Variable> variableMap = new HashMap<String, Variable>();
  private final List<Variable> variables = new ArrayList<Variable>();
  private Variable runVariable;

  /** these variables' values each get their own row. */
  private final List<Variable> rVariables = new ArrayList<Variable>();

  /** this variable's values each get their own column. */
  private Variable cVariable;
  private final List<Value> cValues = new ArrayList<Value>();
  /** An index of results by the variables involved in measuring it. */
  private final Map<Key, Datapoint> keysToDatapoints = new HashMap<Key, Datapoint>();

  private MeasurementType selectedType = MeasurementType.TIME;
  private List<MeasurementType> orderedMeasurementTypes = Arrays.asList(MeasurementType.values());
  private Map<MeasurementType, Integer> divideByMap;
  private Map<MeasurementType, String> unitMap;
  private Map<MeasurementType, Boolean> useRawMap;
  private Map<MeasurementType, Double> maxMap;
  private Map<MeasurementType, Double> minMap;
  private Map<MeasurementType, Double> referencePointMap;
  private Map<MeasurementType, NumberFormat> numberFormatMap;
  private final NumberFormat percentFormat = NumberFormat.getPercentFormat();

  /** We allow the user to toggle between linear and logarithmic bar charts. */
  private boolean logarithmic = false;

  /** We allow the user to toggle between HTML and plaintext */
  private boolean plainText;

  private Benchmark benchmark = null;
  private BenchmarkMeta benchmarkMeta = null;

  public BenchmarkDataViewer(boolean editable, String benchmarkOwner, String benchmarkName,
      long snapshotId, RootPanel snapshotDisclaimerDiv, RootPanel resultsDiv,
      RootPanel fixedVariablesDiv, RootPanel runsDiv, RootPanel environmentsDiv,
      RootPanel snapshotsDiv) {
    this.editable = editable;
    this.benchmarkOwner = benchmarkOwner;
    this.benchmarkName = benchmarkName;
    this.snapshotId = snapshotId;
    this.snapshotDisclaimerDiv = snapshotDisclaimerDiv;
    this.resultsDiv = resultsDiv;
    this.fixedVariablesDiv = fixedVariablesDiv;
    this.runsDiv = runsDiv;
    environmentsIndex = new EnvironmentsIndex();
    environmentsTableDisplay =
        new EnvironmentsTableDisplay(this, environmentsDiv, environmentsIndex);
    snapshotsTableDisplay =
        new SnapshotsTableDisplay(snapshotsDiv);
  }

  public void setBenchmarkMeta(BenchmarkMeta benchmarkMeta) {
    if (isSnapshot()) {
      if (benchmarkMeta == null) {
        throw new IllegalArgumentException("invalid snapshot id " + snapshotId);
      }
    }
    this.benchmark = benchmarkMeta.getBenchmark();
    this.benchmarkMeta = benchmarkMeta;
    this.runMetas.clear();

    if (benchmark.getRuns().isEmpty()) {
      resultsDiv.clear();
      resultsDiv.add(new Label("No runs."));
      fixedVariablesDiv.clear();
      fixedVariablesDiv.add(new Label("No runs."));
      runsDiv.clear();
      runsDiv.add(new Label("No runs."));
      environmentsTableDisplay.setNoRuns();
      rebuildSnapshotsTable();
      return;
    }

    this.runMetas.addAll(benchmark.getRuns());
    environmentsIndex.setRunMetas(this.runMetas);

    rebuildVariables(benchmark);
    rebuildIndex();
    rebuildValueIndices();
    rebuildCValues();

    rebuildSnapshotDisclaimer();
    rebuildResultsTable();
    rebuildVariablesTable();
    rebuildRunsTable();
    rebuildEnvironmentsTable();
    rebuildSnapshotsTable();
  }

  public void rebuildEnvironmentsTable() {
    environmentsTableDisplay.rebuild(editable);
  }

  public void rebuildSnapshotsTable() {
    snapshotsTableDisplay.rebuild(editable, benchmarkMeta, snapshotId);
  }

  private void rebuildSnapshotDisclaimer() {
    snapshotDisclaimerDiv.clear();
    if (isSnapshot()) {
      DateTimeFormat format = DateTimeFormat.getFormat("yyyy-MM-dd 'at' HH:mm:ss Z");
      long createdEpochTime = 0;
      for (BenchmarkSnapshotMeta snapshot : benchmarkMeta.getSnapshots()) {
        if (snapshot.getId() == snapshotId) {
          createdEpochTime = snapshot.getCreated();
        }
      }
      String formattedDate = format.format(new Date(createdEpochTime));
      snapshotDisclaimerDiv.add(
          new HTML("This is a snapshot taken on " + formattedDate
              + " (<a target=\"_blank\" href=\"/run/" + URL.encode(benchmarkOwner) + "/"
              + URL.encode(benchmarkName) + "\">Original</a>)"));
    }
  }

  /**
   * Rebuilds the data structure that knows which variables exist and what their
   * range of values are. Also sets up initial ordering of variables.
   */
  private void rebuildVariables(Benchmark benchmark) {
    Map<String, Variable> allVariables = indexVariables();

    variableMap.clear();
    variableMap.putAll(allVariables);
    variables.clear();
    variables.addAll(allVariables.values());

    // if we've already built the rVariables, copy that set
    if (!rVariables.isEmpty()) {
      for (int i = 0; i < rVariables.size(); i++) {
        rVariables.set(i, allVariables.get(rVariables.get(i).getName()));
      }
      if (cVariable != null) {
        cVariable = allVariables.get(cVariable.getName());
      }

    // if the server provided a list of rVariables, use those
    } else if (benchmark.getRVariables() != null) {
      for (String variableName : benchmark.getRVariables()) {
        this.rVariables.add(allVariables.get(variableName));
      }
      if (benchmark.getCVariable() != null) {
        cVariable = allVariables.get(benchmark.getCVariable());
      }

    // otherwise, come up with a reasonable default
    } else {
      cVariable = runVariable;
    }

    // make sure all variables are represented
    for (Variable variable : variables) {
      if (!rVariables.contains(variable) && cVariable != variable) {
        rVariables.add(variable);
      }
    }

    // hide variables that have only one value
    for (Iterator<Variable> v = rVariables.iterator(); v.hasNext(); ) {
      if (!v.next().hasMultipleValues()) {
        v.remove();
      }
    }
    if (cVariable != null && !cVariable.hasMultipleValues()) {
      cVariable = null;
    }

    rebuildCValues();
  }

  /**
   * We consider the median element of a three-measurement MeasurementSet to be as good a
   * representation of the measurements as any of the three, so throw away the min and max.
   *
   * This ensures that any maximums and minimums kept are representative of what's displayed
   * in the results table.
   *
   * An alternative to this would be to have this.maxDisplayed and this.minDisplayed in addition
   * to this.max and this.min.
   */
  private ScenarioResult normalize(ScenarioResult scenarioResults) {
    Map<MeasurementType, MeasurementSet> measurementSetMap =
        new HashMap<MeasurementType, MeasurementSet>();
    for (MeasurementType measurementType : orderedMeasurementTypes) {
      MeasurementSet measurementSet = scenarioResults.getMeasurementSet(measurementType);
      if (measurementSet == null) {
        continue;
      }

      if (measurementSet.size() > 3) {
        measurementSetMap.put(measurementType, measurementSet);
      } else {
        measurementSetMap.put(measurementType, new MeasurementSet(new Measurement(
            measurementSet.getUnitNames(DEFAULT_UNITS.get(measurementType)),
            measurementSet.medianRaw(),
            measurementSet.medianUnits())));
      }
    }
    return new ScenarioResult(
        measurementSetMap.get(MeasurementType.TIME),
        scenarioResults.getEventLog(MeasurementType.TIME),
        measurementSetMap.get(MeasurementType.INSTANCE),
        scenarioResults.getEventLog(MeasurementType.INSTANCE),
        measurementSetMap.get(MeasurementType.MEMORY),
        scenarioResults.getEventLog(MeasurementType.MEMORY));
  }

  private Map<String, Variable> indexVariables() {
    Map<String, Variable> allVariables = new LinkedHashMap<String, Variable>();
    Map<String, Map<String, Boolean>> variableValuesShown = benchmark.getVariableValuesShown();
    int numVariables = 0;

    // create the special variable for the run
    runVariable = new Variable("run", numVariables++) {
      @Override public Value get(RunMeta runMeta, Scenario scenario) {
        return get(String.valueOf(runMeta.getId()));
      }
    };
    allVariables.put("run", runVariable); // TODO: something less likely to conflict with userspace

    // create the other variables by inspecting the runs
    for (final RunMeta runMeta : runMetas) {
      Run run = runMeta.getRun();
      Value runValue = new Value(String.valueOf(runMeta.getId())) {
        @Override public String getLabel() {
          return runMeta.getName(); // we override this because the label can change
        }
      };
      if (variableValuesShown != null) {
        Map<String, Boolean> runValuesShown = variableValuesShown.get("run");
        if (runValuesShown != null) {
          Boolean isShown = runValuesShown.get(runValue.getName());
          runValue.setShown(isShown == null ? true : isShown);
        }
      }
      runVariable.addValue(runValue);

      for (Map.Entry<Scenario, ScenarioResult> kv : run.getMeasurements().entrySet()) {
        Scenario scenario = kv.getKey();

        for (Map.Entry<String, String> entry : scenario.getVariables().entrySet()) {
          String name = entry.getKey();
          String valueName = entry.getValue();

          Variable variable = allVariables.get(name);
          if (variable == null) {
            variable = new Variable(name, numVariables++);
            allVariables.put(name, variable);
          }

          Value value = variable.addValue(valueName);
          if (variableValuesShown != null) {
            Map<String, Boolean> valuesShown = variableValuesShown.get(name);
            if (valuesShown != null) {
              Boolean isShown = valuesShown.get(valueName);
              value.setShown(isShown == null ? true : isShown);
            }
          }
        }
      }
    }
    return allVariables;
  }

  private void rebuildValueIndices() {
    for (Variable variable : variables) {
      for (Value value : variable.getValues()) {
        value.resetIndex();
      }
    }
    for (Value value : runVariable.getValues()) {
      value.resetIndex();
    }

    for (final RunMeta runMeta : runMetas) {
      Value runValue = runVariable.get(String.valueOf(runMeta.getId()));
      for (Map.Entry<Scenario, ScenarioResult> kv : runMeta.getRun().getMeasurements().entrySet()) {
        Scenario scenario = kv.getKey();
        ScenarioResult scenarioResults = normalize(kv.getValue());

        boolean isScenarioShown = runValue.isShown();
        for (Map.Entry<String, String> entry : scenario.getVariables().entrySet()) {
          String name = entry.getKey();
          String valueName = entry.getValue();
          Variable variable = variableMap.get(name);
          Value value = variable.get(valueName);
          isScenarioShown = isScenarioShown && value.isShown();
        }
        if (!isScenarioShown) {
          continue;
        }

        MeasurementSet measurementSet = scenarioResults.getMeasurementSet(selectedType);
        if (measurementSet != null) {
          runValue.index(measurementSet, useRawMap.get(selectedType));
          for (Map.Entry<String, String> entry : scenario.getVariables().entrySet()) {
            String name = entry.getKey();
            String valueName = entry.getValue();
            Variable variable = variableMap.get(name);
            Value value = variable.get(valueName);
            value.index(measurementSet, useRawMap.get(selectedType));
          }
        }
      }
    }
  }

  /**
   * Rebuilds the index from the combinations of variables to the
   * corresponding datapoint.
   */
  private void rebuildIndex() {
    this.unitMap = new HashMap<MeasurementType, String>();
    this.divideByMap = new HashMap<MeasurementType, Integer>();
    this.numberFormatMap = new HashMap<MeasurementType, NumberFormat>();
    this.maxMap = new HashMap<MeasurementType, Double>();
    this.minMap = new HashMap<MeasurementType, Double>();
    this.referencePointMap = new HashMap<MeasurementType, Double>();
    this.useRawMap = new HashMap<MeasurementType, Boolean>();
    for (MeasurementType measurementType : orderedMeasurementTypes) {
      if (measurementType == MeasurementType.DEBUG) {
        continue;
      }

      double min = Double.POSITIVE_INFINITY;
      double max = 0;

      // select the units to use - default to ns/us/ms/s if there are any differences in the
      // user-defined units between runs.
      Map<String, Integer> units = null;
      boolean useRaw = false;
      UNIT_SELECTION_LOOP: for (RunMeta runMeta : runMetas) {
        for (ScenarioResult scenarioResults : runMeta.getRun().getMeasurements().values()) {
          MeasurementSet measurementSet = scenarioResults.getMeasurementSet(measurementType);
          // if we have no measurement for this run, just skip this run.
          if (measurementSet == null) {
            continue UNIT_SELECTION_LOOP;
          }
          if (units == null) {
            units = measurementSet.getUnitNames();
          } else if (!units.equals(measurementSet.getUnitNames())) {
            useRaw = true;
            units = DEFAULT_UNITS.get(measurementType);
            break UNIT_SELECTION_LOOP;
          }
        }
      }
      useRawMap.put(measurementType, useRaw);
      if (units == null) {
        units = DEFAULT_UNITS.get(measurementType);
      }

      keysToDatapoints.clear();
      for (RunMeta runMeta : runMetas) {
        for (Map.Entry<Scenario, ScenarioResult> entry
            : runMeta.getRun().getMeasurements().entrySet()) {
          ScenarioResult scenarioResult = normalize(entry.getValue());

          Scenario scenario = entry.getKey();
          Key key = new Key(variables.size());

          boolean isShown = true;
          for (Variable variable : variables) {
            Value value = variable.get(runMeta, scenario);
            if (value != null) {
              isShown = isShown && value.isShown();
            }
            key.set(variable, value);
          }

          if (isShown) {
            MeasurementSet measurementSet = scenarioResult.getMeasurementSet(measurementType);
            if (measurementSet != null) {
              min = Math.min(min, getMin(measurementType, measurementSet));
              max = Math.max(max, getMax(measurementType, measurementSet));
            }
          }

          keysToDatapoints.put(key, new Datapoint(scenarioResult, runMeta.getStyle()));
        }
      }

      this.maxMap.put(measurementType, max);
      this.minMap.put(measurementType, min);
      this.referencePointMap.put(measurementType, min);

      List<Map.Entry<String, Integer>> entries =
          new ArrayList<Map.Entry<String, Integer>>(units.entrySet());

      // sort entries in reverse order
      Collections.sort(entries, new Comparator<Map.Entry<String, Integer>>() {
        @Override public int compare(Map.Entry<String, Integer> a, Map.Entry<String, Integer> b) {
          return b.getValue().compareTo(a.getValue());
        }
      });

      int numDigitsInMin = ceil(Math.log10(min));
      String unitCandidate = null;
      for (Map.Entry<String, Integer> entry : entries) {
        if (min / entry.getValue() >= 1) {
          unitCandidate = entry.getKey();
          break;
        }
      }
      if (unitCandidate == null) {
        // if no unit works, just use the smallest available unit.
        unitCandidate = entries.get(entries.size() - 1).getKey();
      }

      unitMap.put(measurementType, unitCandidate);
      divideByMap.put(measurementType, units.get(unitCandidate));
      int decimalDigits = ceil(Math.max(0, Math.log10(divideByMap.get(measurementType))
          + DIGITS_OF_PRECISION - numDigitsInMin));

      String format = "#,###,##0";
      if (decimalDigits > 0) {
        format += ".";
        for (int i = 0; i < decimalDigits; i++) {
          format += "0";
        }
      }
      numberFormatMap.put(measurementType, NumberFormat.getFormat(format));
    }
  }

  private double getMin(MeasurementType measurementType, MeasurementSet measurementSet) {
    return useRawMap.get(measurementType) ? measurementSet.minRaw() : measurementSet.minUnits();
  }

  private double getMax(MeasurementType measurementType, MeasurementSet measurementSet) {
    return useRawMap.get(measurementType) ? measurementSet.maxRaw() : measurementSet.maxUnits();
  }

  private double getMedian(MeasurementType measurementType, MeasurementSet measurementSet) {
    return useRawMap.get(measurementType) ? measurementSet.medianRaw() : measurementSet.medianUnits();
  }

  private Collection<Value> rebuildCValues() {
    cValues.clear();

    if (cVariable != null) {
      for (Value value : cVariable.getValues()) {
        if (value.isShown()) {
          cValues.add(value);
        }
      }
    } else {
      cValues.add(null);
    }
    return cValues;
  }

  private static int ceil(double d) {
    return (int) Math.ceil(d);
  }

  public void rebuildResultsTable() {
    if (plainText) {
      Label label = new Label();
      label.setStyleName("plaintext");
      label.setText(gridToString(toGrid()));

      resultsDiv.clear();
      resultsDiv.add(label);
      resultsDiv.add(new PlainTextEditor().getWidget());
      HTML dash = new HTML(" - ", false);
      dash.setStyleName("inline");
      resultsDiv.add(dash);
      resultsDiv.add(new SnapshotCreator().getWidget());
      return;
    }

    FlexTable table = new FlexTable();
    table.setStyleName("data");
    int r = 0;
    int c = 0;
    int evenRowMod = 0;

    // results header #1: cValue variables
    if (cVariable != null) {
      evenRowMod = 1;
      table.insertRow(r);
      table.getRowFormatter().setStyleName(r, "valueRow");
      table.getRowFormatter().addStyleName(r, "headerRow");

      table.addCell(r);
      table.getFlexCellFormatter().setColSpan(r, 0, rVariables.size());
      c++;
      for (Value cValue : cValues) {
        table.addCell(r);
        table.getFlexCellFormatter().setColSpan(r, c, 3);
        table.getCellFormatter().setStyleName(r, c, "parameterKey");

        Widget contents = newVariableLabel(cVariable, cValue.getLabel(), rVariables.size());
        contents.setStyleName("valueHeader");

        table.setWidget(r, c++, contents);
      }
      r++;
    }

    // results header 2: rValue variables, followed by "nanos/barchart" column pairs
    c = 0;
    table.insertRow(r);
    table.getRowFormatter().setStyleName(r, "evenRow");
    table.getRowFormatter().addStyleName(r, "headerRow");
    for (Variable variable : rVariables) {
      table.addCell(r);
      table.getCellFormatter().setStyleName(r, c, "parameterKey");
      table.setWidget(r, c, newVariableLabel(variable, variable.getName(), c));
      c++;
    }
    for (Value unused : cValues) {
      table.addCell(r);
      table.getCellFormatter().setStyleName(r, c, "parameterKey");
      table.setWidget(r, c++, newUnitLabel(unitMap.get(selectedType).trim()));

      table.addCell(r);
      table.getCellFormatter().setStyleName(r, c, "parameterKey");
      table.setWidget(r, c++, newRuntimeLabel());

      table.addCell(r);
      table.getCellFormatter().setStyleName(r, c, "parameterKey");
      table.setWidget(r, c++, new InlineLabel("%"));
    }
    r++;

    Key key = newDefaultKey();
    for (RowsIterator rows = new RowsIterator(rVariables); rows.nextRow(); ) {
      rows.updateKey(key);

      table.insertRow(r);
      table.getRowFormatter().setStyleName(r, r % 2 == evenRowMod ? "evenRow" : "oddRow");
      c = 0;
      for (int v = 0, size = rVariables.size(); v < size; v++) {
        table.addCell(r);
        table.setWidget(r, c++, new Label(rows.getRValue(v).getLabel()));
      }

      for (Value value : cValues) {
        table.addCell(r);
        table.addCell(r);

        if (cVariable != null) {
          key.set(cVariable, value);
        }

        final Datapoint datapoint = keysToDatapoints.get(key);
        table.getCellFormatter().setStyleName(r, c, "numericCell");
        table.getCellFormatter().setStyleName(r, c + 1, "bar");
        table.getCellFormatter().setStyleName(r, c + 2, "numericCell");
        MeasurementSet measurementSet;
        if (datapoint != null &&
            (measurementSet = datapoint.scenarioResults.getMeasurementSet(selectedType)) != null) {
          double rawMedian = getMedian(selectedType, measurementSet);
          String displayedValue = numberFormatMap.get(selectedType)
              .format(rawMedian / divideByMap.get(selectedType));
          Anchor valueAnchor = new Anchor(displayedValue, false);
          valueAnchor.setStyleName("subtleLink");
          valueAnchor.setStyleName("nanos", true);

          final DialogBox eventLogPopup = new DialogBox(true);
          eventLogPopup.setText("");

          valueAnchor.addClickHandler(new ClickHandler() {
            public void onClick(ClickEvent clickEvent) {
              // Do this lazily since it takes quite a bit of time to render these popups for all
              // the scenarios shown, and quite often they won't even be used.
              if (eventLogPopup.getText().isEmpty()) {
                eventLogPopup.setText("Event Log");
                String eventLog = datapoint.scenarioResults.getEventLog(selectedType);
                if (eventLog == null || eventLog.isEmpty()) {
                  eventLog = "No event log recorded.";
                }
                FlowPanel panel = new FlowPanel();
                for (String line : eventLog.split("\n")) {
                  panel.add(new Label(line));
                }
                panel.setStyleName("eventLog");
                eventLogPopup.add(panel);
              }
              eventLogPopup.center();
              eventLogPopup.show();
            }
          });

          table.setWidget(r, c, valueAnchor);
          table.setWidget(r, c + 1, newBar(datapoint.style, measurementSet, value));
          table.setWidget(r, c + 2, newPercentOfReferencePointLabel(rawMedian, value));
        } else {
          table.setWidget(r, c, new Label(""));
          table.setWidget(r, c + 1, new Label(""));
          table.setWidget(r, c + 2, new Label(""));
        }
        c += 3;
      }

      r++;
    }
    resultsDiv.clear();
    resultsDiv.add(table);
    resultsDiv.add(new PlainTextEditor().getWidget());
    HTML dash = new HTML(" - ", false);
    dash.setStyleName("inline");
    resultsDiv.add(dash);
    resultsDiv.add(new SnapshotCreator().getWidget());
  }

  class SnapshotCreator implements ClickHandler {
    private static final String SAVING_TEXT = "Saving...";
    private static final String DEFAULT_TEXT = "Create Snapshot";
    private static final String FAILED_TEXT = "Failed";

    private final Anchor anchor;

    private SnapshotCreator() {
      anchor = new Anchor(DEFAULT_TEXT);
      anchor.addClickHandler(this);
      anchor.setStyleName("actionLink");
    }

    public void onClick(ClickEvent clickEvent) {
      anchor.setText(SAVING_TEXT);
      BenchmarkServiceAsync benchmarkService = GWT.create(BenchmarkService.class);
      if (benchmark != null) {
        List<String> rVariableNames = new ArrayList<String>();
        for (Variable rVariable : rVariables) {
          rVariableNames.add(rVariable.getName());
        }
        String cVariableName = null;
        if (cVariable != null) {
          cVariableName = cVariable.getName();
        }
        Benchmark benchmarkToSave =
            new Benchmark(benchmarkOwner, benchmarkName, runMetas, rVariableNames, cVariableName,
                variableValuesShown());
        benchmarkService.createSnapshot(benchmarkToSave, new AsyncCallback<Long>() {
          public void onFailure(Throwable throwable) {
            anchor.setText(FAILED_TEXT);
          }

          public void onSuccess(Long snapshotId) {
            anchor.setText(DEFAULT_TEXT);
            // open a new tab/window with the snapshot
            Window.open(
                "/run/" + benchmarkOwner + "/" + benchmarkName + "/" + snapshotId, "_blank", "");
          }
        });
      }
    }

    public Anchor getWidget() {
      return anchor;
    }
  }

  class PlainTextEditor implements ClickHandler {
    private final Anchor anchor;

    private PlainTextEditor() {
      anchor = new Anchor(plainText ? "HTML" : "Plain Text");
      anchor.addClickHandler(this);
      anchor.setStyleName("actionLink");
    }

    public void onClick(ClickEvent clickEvent) {
      plainText = !plainText;
      rebuildResultsTable();
    }

    public Anchor getWidget() {
      return anchor;
    }
  }

  /**
   * Returns a grid containing the raw contents of the results table. Cells are
   * either string labels, integers or measurement sets.
   */
  public List<List<Object>> toGrid() {
    List<List<Object>> result = new ArrayList<List<Object>>();

    // results header #1: cValue variables
    if (cVariable != null) {
      List<Object> header1 = new ArrayList<Object>();
      for (Object unused : rVariables) {
        header1.add("");
      }
      for (Value cValue : cValues) {
        header1.add("");
        header1.add(cValue.getLabel());
        header1.add("");
      }
      result.add(header1);
    }

    // results header 2: rValue variables, followed by nanos, barchart, %
    List<Object> header2 = new ArrayList<Object>();
    for (Variable variable : rVariables) {
      header2.add(variable.getName());
    }
    for (Value unused : cValues) {
      header2.add(unitMap.get(selectedType).trim());
      header2.add(logarithmic ? "logarithmic runtime" : "linear runtime");
      header2.add("%");
    }
    result.add(header2);

    Key key = newDefaultKey();
    for (RowsIterator rows = new RowsIterator(rVariables); rows.nextRow(); ) {
      rows.updateKey(key);
      List<Object> row = new ArrayList<Object>();

      for (int v = 0, size = rVariables.size(); v < size; v++) {
        row.add(rows.getRValue(v).getLabel());
      }

      for (Value value : cValues) {
        if (cVariable != null) {
          key.set(cVariable, value);
        }

        Datapoint datapoint = keysToDatapoints.get(key);
        MeasurementSet measurementSet;
        if (datapoint == null ||
            (measurementSet = datapoint.scenarioResults.getMeasurementSet(selectedType)) == null) {
          // sparse dataset
          row.add("");
          row.add("");
          row.add("");
        } else {
          double rawMedian = getMedian(selectedType, measurementSet);
          row.add(rawMedian / divideByMap.get(selectedType));
          row.add(measurementSet);
          row.add(percentFormat.format(getRatio(rawMedian, value)));
        }
      }
      result.add(row);
    }

    return result;
  }

  public String gridToString(List<List<Object>> grid) {
    List<Object> firstRow = grid.get(0);

    // compute max and min values per column
    double[] minValues = new double[firstRow.size()];
    for (int i = 0; i < minValues.length; i++) {
      minValues[i] = Double.POSITIVE_INFINITY;
    }
    double[] maxValues = new double[firstRow.size()];
    for (List<Object> row : grid) {
      int c = 0;
      for (Object cell : row) {
        if (cell instanceof MeasurementSet) {
          double value = getMedian(selectedType, ((MeasurementSet) cell));
          minValues[c] = Math.min(minValues[c], value);
          maxValues[c] = Math.max(maxValues[c], value);
        }
        c++;
      }
    }

    // turn the grid of objects into a grid of strings
    int[] maxColumnWidths = new int[firstRow.size()];
    String[][] stringsGrid = new String[grid.size()][firstRow.size()];
    Map<Integer, Boolean> isMeasurementSetColumn = new HashMap<Integer, Boolean>();
    int r = 0;
    for (List<Object> row : grid) {
      int c = 0;
      for (Object cell : row) {
        if (cell instanceof String) {
          stringsGrid[r][c] = (String) cell;
        } else if (cell instanceof Double) {
          stringsGrid[r][c] = numberFormatMap.get(selectedType).format((Double) cell);
        } else if (cell instanceof MeasurementSet) {
          isMeasurementSetColumn.put(c, true);
          stringsGrid[r][c] =
              asciiArtBar(getMedian(selectedType, ((MeasurementSet) cell)), minValues[c],
                  maxValues[c]);
        }

        maxColumnWidths[c] = Math.max(maxColumnWidths[c], stringsGrid[r][c].length());
        c++;
      }
      r++;
    }

    // create one big string
    StringBuilder result = new StringBuilder();
    r = 0;
    for (String[] row : stringsGrid) {
      int c = 0;
      for (String cell : row) {
        boolean alignLeft = isMeasurementSetColumn.get(c) != null;
        int padding = maxColumnWidths[c] - cell.length();
        if (alignLeft) {
          result.append(cell);
          result.append(repeat(" ", padding));
        } else {
          result.append(repeat(" ", padding));
          result.append(cell);
        }
        if (c < firstRow.size()) {
          result.append(" ");
        }
        c++;
      }
      result.append("\n");
      r++;
    }
    return result.toString();
  }

  private String repeat(String value, int times) {
    StringBuilder result = new StringBuilder();
    for (int i = 0; i < times; i++) {
      result.append(value);
    }
    return result.toString();
  }

  private String asciiArtBar(double value, double minValue, double maxValue) {
    String bar = "==============================";
    int width;
    if (logarithmic && Math.abs(minValue - maxValue) > 1.0E-6) {
      LinearTranslation translation = new LinearTranslation(minValue, 1, maxValue, bar.length());
      width = Math.max(Math.min((int) translation.translate(value), bar.length()), 1);
    } else {
      width = (int) (bar.length() * value / maxValue);
    }
    return bar.substring(0, width);
  }

  private Key newDefaultKey() {
    Key key = new Key(variables.size());
    for (Variable v : variables) {
      if (!v.hasMultipleValues()) {
        key.set(v, v.getOnlyValue());
      }
    }
    return key;
  }

  /**
   * @param variable the variable to label. If null, no variable is being labelled.
   * @param rIndex the column the labelled variable will be displayed in, or
   *      rVariables.size() if this is a cVariable.
   */
  private Widget newVariableLabel(final Variable variable, String label, final int rIndex) {
    if (variable == null) {
      return new InlineLabel(label);
    }

    Panel panel = new FlowPanel();

    if (rIndex > 0 || rIndex == rVariables.size()) {
      Anchor shiftLeft = new Anchor("\u2190");
      shiftLeft.setStyleName("hiddentoggle");
      shiftLeft.addClickHandler(new ClickHandler() {
        public void onClick(ClickEvent clickEvent) {
          moveVariable(variable, rIndex, rIndex - 1);
        }
      });
      panel.add(shiftLeft);
    }

    panel.add(new InlineLabel("\u00A0" + label + "\u00A0"));

    if (rIndex < rVariables.size()) {
      Anchor shiftRight = new Anchor("\u2192");
      shiftRight.setStyleName("hiddentoggle");
      shiftRight.addClickHandler(new ClickHandler() {
        public void onClick(ClickEvent clickEvent) {
          moveVariable(variable, rIndex, rIndex + 1);
        }
      });
      panel.add(shiftRight);
    }

    return panel;
  }

  void moveVariable(Variable variable, int oldIndex, int newIndex) {
    // we're demoting the cVariable to an rVariable
    if (variable == cVariable) {
      rVariables.add(variable);
      cVariable = null;

    // we're promoting an rVariable to a cVariable
    } else if (newIndex == rVariables.size()) {
      rVariables.remove(oldIndex);
      if (cVariable != null) {
        rVariables.add(cVariable); // demote the current cVariable; we can have at most one
      }
      cVariable = variable;

    // we're reordering the rVariables
    } else {
      rVariables.remove(oldIndex);
      rVariables.add(newIndex, variable);
    }

    rebuildCValues();
    rebuildResultsTable();

    if (editable) {
      List<String> rVariableNames = new ArrayList<String>();
      for (Variable rVariable : rVariables) {
        rVariableNames.add(rVariable.getName());
      }
      String cVariableName = null;
      if (cVariable != null) {
        cVariableName = cVariable.getName();
      }
      BenchmarkServiceAsync benchmarkService = GWT.create(BenchmarkService.class);
      benchmarkService.reorderVariables(benchmarkOwner, benchmarkName,
          rVariableNames, cVariableName, NO_OP_CALLBACK);
    }
  }

  private void cycleSelectedType() {
    selectedType = orderedMeasurementTypes.get(
        (orderedMeasurementTypes.indexOf(selectedType) + 1) % orderedMeasurementTypes.size());
  }

  private Widget newUnitLabel(String unit) {
    Anchor result = new Anchor(unit);
    result.setStyleName("visibletoggle");
    result.addClickHandler(new ClickHandler() {
      public void onClick(ClickEvent clickEvent) {
        cycleSelectedType();
        rebuildValueIndices();
        rebuildCValues();
        rebuildResultsTable();
      }
    });
    return result;
  }

  private Widget newRuntimeLabel() {
    String label = logarithmic ? "logarithmic runtime" : "linear runtime";
    Anchor result = new Anchor(label);
    result.setStyleName("visibletoggle");
    result.addClickHandler(new ClickHandler() {
      public void onClick(ClickEvent clickEvent) {
        logarithmic = !logarithmic;
        rebuildResultsTable();
      }
    });
    return result;
  }

  private MeasurementSet log(MeasurementSet measurementSet) {
    List<Measurement> measurements = new ArrayList<Measurement>();
    for (Measurement oldMeasurement : measurementSet.getMeasurements()) {
      measurements.add(new Measurement(
          measurementSet.getUnitNames(),
          Math.log(oldMeasurement.getRaw()),
          Math.log(oldMeasurement.getProcessed())));
    }
    return new MeasurementSet(measurements.toArray(new Measurement[measurements.size()]));
  }

  /**
   * Returns a measurement set that has had either its user-defined unit values or raw values
   * translated by {@code translation}, depending on whether we're using raw values.
   *
   * Unlike log, we do not simply translate both raw and processed values since the translation
   * passed in is in terms of one or the other.
   */
  private MeasurementSet translate(boolean raw, MeasurementSet measurementSet,
      LinearTranslation translation) {
    List<Measurement> measurements = new ArrayList<Measurement>();
    for (Measurement oldMeasurement : measurementSet.getMeasurements()) {
      if (raw) {
        measurements.add(new Measurement(
            measurementSet.getUnitNames(),
            translation.translate(oldMeasurement.getRaw()),
            oldMeasurement.getProcessed()));
      } else {
        measurements.add(new Measurement(
            measurementSet.getUnitNames(),
            oldMeasurement.getRaw(),
            translation.translate(oldMeasurement.getProcessed())));
      }
    }
    return new MeasurementSet(measurements.toArray(new Measurement[measurements.size()]));
  }

  private BoxPlot boxPlot = new BoxPlot(MAX_BAR_WIDTH, MAX_BAR_HEIGHT);

  private Widget newBar(int style, MeasurementSet measurementSet, Value cValue) {
    double max;
    double min;
    if (cVariable != null) {
      max = cValue.getMax();
      min = cValue.getMin();
    } else {
      max = this.maxMap.get(selectedType);
      min = this.minMap.get(selectedType);
    }

    if (logarithmic) {
      min = Math.log(min);
      max = Math.log(max);
      if (Math.abs(min - max) > 1.0E-6) {
        measurementSet = translate(useRawMap.get(selectedType), log(measurementSet),
            new LinearTranslation(min, 1, max, 100));
        max = 100;
      }
    }

    return boxPlot.create(style, max, measurementSet, useRawMap.get(selectedType));
  }

  private double getRatio(double measurement, Value value) {
    double referencePoint = value != null
        ? value.getReferencePoint()
        : referencePointMap.get(selectedType);
    return measurement / referencePoint;
  }

  /**
   * A clickable label displaying something like "50%".
   */
  private Widget newPercentOfReferencePointLabel(final double measurement, final Value value) {
    final Anchor result = new Anchor(percentFormat.format(getRatio(measurement, value)));
    result.setStyleName("subtleLink");
    result.addClickHandler(new ClickHandler() {
      public void onClick(ClickEvent clickEvent) {
        if (value != null) {
          value.setReferencePoint(measurement);
        } else {
          referencePointMap.put(selectedType, measurement);
        }
        rebuildResultsTable();
      }
    });
    return result;
  }

  public void rebuildVariablesTable() {
    FlexTable table = new FlexTable();
    table.setStyleName("data");

    int r = 0;
    for (Variable variable : variables) {
      table.insertRow(r);

      table.addCell(r);
      table.getRowFormatter().setStyleName(r, r % 2 == 0 ? "evenRow" : "oddrow");
      table.getCellFormatter().setStyleName(r, 0, "fixedParameterKey");
      table.setWidget(r, 0, new Label(variable.getName()));

      table.addCell(r);
      FlowPanel checkBoxes = new FlowPanel();
      if (variable.hasMultipleValues()) {
        for (Value value : variable.getValues()) {
          checkBoxes.add(newShownCheckbox(value));
        }
      } else {
        checkBoxes.add(new Label(variable.getOnlyValue().getName()));
      }
      table.setWidget(r, 1, checkBoxes);

      r++;
    }

    fixedVariablesDiv.clear();
    fixedVariablesDiv.add(table);
  }

  public void rebuildRunsTable() {
    int columns = 4;
    if (editable) {
      columns += 1;
    }

    Grid table = new Grid(runMetas.size() + 1, columns);
    table.setStyleName("data");

    int r = 0;
    table.getRowFormatter().setStyleName(r, "evenRow");
    table.getCellFormatter().setStyleName(r, 1, "parameterKey");
    table.setWidget(r, 1, new Label("run"));
    table.getCellFormatter().setStyleName(r, 2, "parameterKey");
    table.setWidget(r, 2, new Label("executed"));
    table.getCellFormatter().setStyleName(r, 3, "parameterKey");
    table.setWidget(r, 3, new Label("environment"));
    r++;

    AsyncCallback<Void> runNameEditorCallback = new AsyncCallback<Void>() {
      public void onFailure(Throwable throwable) {
        // do nothing
      }

      public void onSuccess(Void unused) {
        rebuildResultsTable();
        rebuildVariablesTable();
      }
    };

    DateTimeFormat format = DateTimeFormat.getFormat("EEE MMM dd HH:mm:ss Z yyyy");
    for (RunMeta runMeta : runMetas) {
      Run run = runMeta.getRun();

      table.getRowFormatter().setStyleName(r, r % 2 == 0 ? "evenRow" : "oddRow");

      Anchor nameEditorLink = new Anchor("");
      nameEditorLink.setStyleName("labelField");

      table.setWidget(r, 0, swatch(runMeta));
      table.setWidget(r, 1, new RunNameEditor(runMeta, nameEditorLink, new Label(""), editable,
          runNameEditorCallback).getWidget());
      table.setWidget(r, 2, new Label(format.format(run.getExecutedTimestamp())));
      table.setWidget(r, 3, newEnvironmentWidget(runMeta));
      if (editable) {
        table.setWidget(r, 4, new RunDeletedEditor(runMeta).getWidget());
      }

      r++;
    }

    runsDiv.clear();
    runsDiv.add(table);
  }

  private Widget newEnvironmentWidget(RunMeta runMeta) {
    Widget environmentWidget;
    EnvironmentMeta environmentMeta = runMeta.getEnvironmentMeta();
    if (environmentMeta == null) {
      Label noEnvironmentLabel = new Label("none");
      noEnvironmentLabel.setStyleName("placeholder");
      environmentWidget = noEnvironmentLabel;
    } else {
      Anchor environmentAnchor =
        new Anchor(environmentMeta.getName(), false, "#environments");
      environmentAnchor.setStyleName("subtlelink");
      environmentWidget = environmentAnchor;
    }
    return environmentWidget;
  }

  private Widget swatch(RunMeta runMeta) {
    Color[] colors = Colors.forStyle(runMeta.getStyle());

    FlowPanel result = new FlowPanel();
    InlineLabel dark = new InlineLabel("....");
    dark.getElement().setAttribute("style",
        "background-color: " + colors[0] + "; color: " + colors[0]);
    InlineLabel medium = new InlineLabel(".");
    medium.getElement().setAttribute("style",
        "background-color: " + colors[1] + "; color: " + colors[1]);
    InlineLabel light = new InlineLabel(".");
    light.getElement().setAttribute("style",
        "background-color: " + colors[2] + "; color: " + colors[2]);

    result.add(dark);
    result.add(medium);
    result.add(light);
    return result;
  }

  private CheckBox newShownCheckbox(final Value value) {
    CheckBox isShown = new CheckBox(value.getLabel());
    isShown.setValue(value.isShown());

    isShown.addValueChangeHandler(new ValueChangeHandler<Boolean>() {
      public void onValueChange(ValueChangeEvent<Boolean> event) {
        value.setShown(!value.isShown());
        rebuildIndex();
        rebuildValueIndices();
        rebuildCValues();
        rebuildResultsTable();
        if (editable) {
          Map<String, Map<String, Boolean>> variableValuesShown = variableValuesShown();
          BenchmarkServiceAsync benchmarkService = GWT.create(BenchmarkService.class);
          benchmarkService.setVariableValuesShown(benchmarkOwner, benchmarkName,
              variableValuesShown, NO_OP_CALLBACK);
        }
      }
    });

    return isShown;
  }

  private Map<String, Map<String, Boolean>> variableValuesShown() {
    Map<String, Map<String, Boolean>> variableValuesShown =
        new HashMap<String, Map<String, Boolean>>();

    for (Variable variable : variables) {
      Map<String, Boolean> valuesShown = new HashMap<String, Boolean>();
      for (Value value : variable.getValues()) {
        valuesShown.put(value.getName(), value.isShown());
      }
      variableValuesShown.put(variable.getName(), valuesShown);
    }
    return variableValuesShown;
  }

  private static class Datapoint {
    final ScenarioResult scenarioResults;
    final int style;

    private Datapoint(ScenarioResult scenarioResults, int style) {
      this.scenarioResults = scenarioResults;
      this.style = style;
    }
  }

  private boolean isSnapshot() {
    return snapshotId != -1;
  }
}
