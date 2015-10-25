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

import com.google.gwt.i18n.client.DateTimeFormat;
import com.google.gwt.user.client.ui.Anchor;
import com.google.gwt.user.client.ui.Grid;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.RootPanel;
import java.util.Collections;
import java.util.Date;
import java.util.List;

public final class SnapshotsTableDisplay {
  private final RootPanel container;

  public SnapshotsTableDisplay(RootPanel container) {
    this.container = container;
  }

  public void rebuild(boolean editable, BenchmarkMeta benchmarkMeta, long selectedSnapshotId) {
    container.clear();
    List<BenchmarkSnapshotMeta> snapshots = benchmarkMeta.getSnapshots();
    Benchmark baseBenchmark = benchmarkMeta.getBenchmark();

    // rows for all snapshots, and the base benchmark itself.
    Grid table = new Grid(snapshots.size() + 1, editable ? 3 : 2);
    table.setStyleName("data");
    int row = 0;
    int column = 0;

    // baseBenchmark
    if (selectedSnapshotId == -1) {
      table.getRowFormatter().setStyleName(row, "environmentDifferRow");
    } else {
      table.getRowFormatter().setStyleName(row, row % 2 == 0 ? "evenRow" : "oddRow");
    }
    table.setWidget(row, column++, new Anchor("Original", false,
        "/run/" + baseBenchmark.getOwner() + "/" + baseBenchmark.getName()));
    table.getCellFormatter().addStyleName(row, column, "placeholder");
    row++;

    Collections.sort(snapshots, Collections.reverseOrder(BenchmarkSnapshotMeta.ORDER_BY_DATE));
    for (BenchmarkSnapshotMeta snapshot : snapshots) {
      column = 0;

      if (selectedSnapshotId == snapshot.getId()) {
        table.getRowFormatter().setStyleName(row, "environmentDifferRow");
      } else {
        table.getRowFormatter().setStyleName(row, row % 2 == 0 ? "evenRow" : "oddRow");
      }
      DateTimeFormat format = DateTimeFormat.getFormat("yyyy-MM-dd HH:mm:ss Z");
      String formattedDate = format.format(new Date(snapshot.getCreated()));

      table.setWidget(row, column++, new Anchor("Snapshot", false,
          "/run/" + snapshot.getBenchmarkOwner() + "/" + snapshot.getBenchmarkName() +
          "/" + snapshot.getId()));
      table.setWidget(row, column++, new Label(formattedDate));
      if (editable) {
        table.setWidget(row, column++, new SnapshotDeletedEditor(snapshot).getWidget());
      }
      row++;
    }

    container.add(table);
  }
}
