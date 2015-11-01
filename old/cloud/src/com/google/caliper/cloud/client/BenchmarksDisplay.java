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

import com.google.gwt.user.client.ui.Anchor;
import com.google.gwt.user.client.ui.Grid;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.RootPanel;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class BenchmarksDisplay {
  private final String benchmarkOwner;
  private final RootPanel benchmarksDiv;

  private List<String> benchmarkNames = new ArrayList<String>();

  public BenchmarksDisplay(String benchmarkOwner, RootPanel benchmarksDiv) {
    this.benchmarkOwner = benchmarkOwner;
    this.benchmarksDiv = benchmarksDiv;
  }

  public void setBenchmarks(List<String> benchmarkNames) {
    this.benchmarkNames.clear();

    this.benchmarkNames.addAll(benchmarkNames);

    if (benchmarkNames.isEmpty()) {
      benchmarksDiv.clear();
      benchmarksDiv.add(new Label("No benchmarks yet."));
      return;
    }

    rebuildBenchmarksTable();
  }

  private void rebuildBenchmarksTable() {
    Grid table = new Grid(benchmarkNames.size() + 1, 2);
    table.setStyleName("data");

    int r = 0;
    table.getRowFormatter().setStyleName(r, "evenRow");
    table.getCellFormatter().setStyleName(r, 0, "parameterKey");
    table.setWidget(r, 0, new Label("package"));
    table.getCellFormatter().setStyleName(r, 1, "parameterKey");
    table.setWidget(r, 1, new Label("benchmark"));
    r++;

    Collections.sort(benchmarkNames);
    for (String benchmarkName : benchmarkNames) {
      table.getRowFormatter().addStyleName(r, r % 2 == 0 ? "evenRow" : "oddRow");

      int lastPeriod = benchmarkName.lastIndexOf('.');
      String packageName;
      String className;
      if (lastPeriod != -1) {
        packageName = benchmarkName.substring(0, lastPeriod);
        className = benchmarkName.substring(lastPeriod + 1);
      } else {
        packageName = "(default package)";
        table.getCellFormatter().setStyleName(r, 0, "placeholder");
        className = benchmarkName;
      }

      table.setWidget(r, 0, new Label(packageName));

      Anchor benchmarkAnchor = new Anchor(className, false, "/run/" + benchmarkOwner + "/" + benchmarkName);
      benchmarkAnchor.addStyleName("strong");
      table.setWidget(r, 1, benchmarkAnchor);
      r++;
    }

    benchmarksDiv.clear();
    benchmarksDiv.add(table);
  }
}
