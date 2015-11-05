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

import com.google.gwt.core.client.GWT;
import com.google.gwt.i18n.client.Dictionary;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.RootPanel;

public class BenchmarkView implements PageView {

  private final BenchmarkServiceAsync benchmarkService = GWT.create(BenchmarkService.class);

  private String user;
  private String benchmarkOwner;
  private String benchmarkName;
  private BenchmarkDataViewer benchmarkDataViewer;
  private long snapshotId;

  public void init() {
    Dictionary globals = Dictionary.getDictionary("pageGlobals");

    user = globals.get("user");
    benchmarkOwner = globals.get("benchmarkOwner");
    benchmarkName = globals.get("benchmarkName");
    String snapshotIdString = globals.get("snapshot");
    if (!snapshotIdString.isEmpty()) {
      try {
        snapshotId = Long.parseLong(globals.get("snapshot"));
      } catch (NumberFormatException e) {
        redirectToBaseBenchmark();
        return;
      }
    } else {
      snapshotId = -1;
    }

    RootPanel snapshotDisclaimerDiv = RootPanel.get("snapshotDisclaimer");
    RootPanel resultsDiv = RootPanel.get("results");
    RootPanel fixedVariablesDiv = RootPanel.get("variables");
    RootPanel runsDiv = RootPanel.get("runs");
    RootPanel environmentsDiv = RootPanel.get("environments");
    RootPanel snapshotsDiv = RootPanel.get("snapshots");

    boolean editable = benchmarkOwner.equals(user) && !isSnapshot();

    benchmarkDataViewer = new BenchmarkDataViewer(editable, benchmarkOwner, benchmarkName,
        snapshotId, snapshotDisclaimerDiv, resultsDiv, fixedVariablesDiv, runsDiv, environmentsDiv,
        snapshotsDiv);
  }

  private void redirectToBaseBenchmark() {
    Window.Location.replace("/run/" + benchmarkOwner + "/" + benchmarkName);
  }

  private boolean isSnapshot() {
    return snapshotId != -1;
  }

  public void reload(final RefreshButton refreshButton) {
    refreshButton.loading();
    Long snapshotParam = snapshotId == -1 ? null : snapshotId;
    benchmarkService.fetchBenchmark(benchmarkOwner, benchmarkName, snapshotParam,
        new AsyncCallback<BenchmarkMeta>() {
          public void onSuccess(BenchmarkMeta benchmarkMeta) {
            try {
              benchmarkDataViewer.setBenchmarkMeta(benchmarkMeta);
            } catch (IllegalArgumentException e) {
              redirectToBaseBenchmark();
              return;
            }
            refreshButton.done();
          }

          public void onFailure(Throwable throwable) {
            refreshButton.failed();
          }
        });
  }
}
