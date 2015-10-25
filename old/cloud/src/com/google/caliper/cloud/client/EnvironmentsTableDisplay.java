/**
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

package com.google.caliper.cloud.client;

import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Anchor;
import com.google.gwt.user.client.ui.Grid;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.RootPanel;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public final class EnvironmentsTableDisplay {

  private final AsyncCallback<Void> environmentNameEditorCallback = new AsyncCallback<Void>() {
      public void onFailure(Throwable throwable) {
        // do nothing
      }

      public void onSuccess(Void unused) {
        environmentsIndex.invalidate();
        benchmarkDataViewer.rebuildRunsTable();
        benchmarkDataViewer.rebuildEnvironmentsTable();
      }
    };

  private final BenchmarkDataViewer benchmarkDataViewer;
  private final RootPanel container;
  private final EnvironmentsIndex environmentsIndex;

  public EnvironmentsTableDisplay(BenchmarkDataViewer benchmarkDataViewer, RootPanel container,
      EnvironmentsIndex environmentsIndex) {
    this.benchmarkDataViewer = benchmarkDataViewer;
    this.container = container;
    this.environmentsIndex = environmentsIndex;
  }

  public void rebuild(boolean editable) {
    container.clear();
    List<EnvironmentMeta> environments = environmentsIndex.getEnvironments();

    if (environments.isEmpty()) {
      container.add(new Label("No environments."));
      return;
    }

    Set<String> allPropertiesSet = new HashSet<String>();
    for (EnvironmentMeta environment : environments) {
      allPropertiesSet.addAll(environment.getEnvironment().getProperties().keySet());
    }

    List<String> allProperties = new ArrayList<String>(allPropertiesSet);

    container.add(createGrid(editable, environments, allProperties));
  }

  private Grid createGrid(boolean editable, List<EnvironmentMeta> environments,
      List<String> allProperties) {
    Collections.sort(environments, EnvironmentMeta.ORDER_BY_DATE);
    Collections.sort(allProperties);

    Grid table = new Grid(allProperties.size() + 1, environments.size() + 1);
    table.setStyleName("data");
    int row = 0;

    // header
    table.getRowFormatter().setStyleName(row, "evenRow");
    int headerColumn = 0;
    for (EnvironmentMeta environment : environments) {
      headerColumn++;

      table.getCellFormatter().setStyleName(row, headerColumn, "parameterKey");
      Anchor nameEditorLink = new Anchor("");
      nameEditorLink.setStyleName("tableHeaderLink");
      table.setWidget(row, headerColumn, new EnvironmentNameEditor(environment, nameEditorLink,
          new Label(""), editable, environmentNameEditorCallback).getWidget());
    }

    // body
    for (String property : allProperties) {
      row++;
      int column = 0;

      // property row
      String rowStyleName = styleNameForPropertyRow(row, hasMultipleValues(environments, property));
      table.getRowFormatter().setStyleName(row, rowStyleName);

      // row title (property name)
      table.getCellFormatter().setStyleName(row, column, "rowLabel");
      table.setWidget(row, column, new Label(property));

      // property value per environment
      for (EnvironmentMeta environment : environments) {
        column++;

        String propertyValue = environment.getEnvironment().getProperties().get(property);
        if (propertyValue == null) {
          propertyValue = "N/A";
          table.getCellFormatter().setStyleName(row, column, "placeholder");
        }

        table.setWidget(row, column, new Label(propertyValue));
      }
    }
    return table;
  }

  private String styleNameForPropertyRow(int row, boolean hasMultipleValues) {
    if (hasMultipleValues) {
      return "environmentDifferRow";
    }
    return row % 2 == 0 ? "evenRow" : "oddRow";
  }

  /**
   * Returns true if two or more environments have different values for property {@code property},
   * or if one environment is missing a value for property {@code property}.
   */
  private boolean hasMultipleValues(List<EnvironmentMeta> environments, String property) {
    String firstPropertyValue = null;
    boolean first = true;
    for (EnvironmentMeta environment : environments) {
      String propertyValue = environment.getEnvironment().getProperties().get(property);

      if (first) {
        firstPropertyValue = propertyValue;
        if (firstPropertyValue == null) {
          return true;
        }
        first = false;
        continue;
      }

      if (!firstPropertyValue.equals(propertyValue)) {
        return true;
      }
    }
    return false;
  }

  public void setNoRuns() {
    container.clear();
    container.add(new Label("No runs."));
  }
}
