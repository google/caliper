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
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.RootPanel;
import java.util.List;

public class UserDashboardView implements PageView {

  private final BenchmarkServiceAsync benchmarkService = GWT.create(BenchmarkService.class);

  private String benchmarkOwner;

  private BenchmarksDisplay benchmarksDisplay;

  public void init() {
    Dictionary globals = Dictionary.getDictionary("pageGlobals");
    benchmarkOwner = globals.get("benchmarkOwner");

    RootPanel benchmarksDiv = RootPanel.get("benchmarks");

    benchmarksDisplay = new BenchmarksDisplay(benchmarkOwner, benchmarksDiv);
  }

  public void reload(final RefreshButton refreshButton) {
    refreshButton.loading();
    benchmarkService.fetchBenchmarkNames(benchmarkOwner, new AsyncCallback<List<String>>() {
      public void onSuccess(List<String> benchmarkNames) {
        refreshButton.done();
        benchmarksDisplay.setBenchmarks(benchmarkNames);
      }

      public void onFailure(Throwable throwable) {
        refreshButton.failed();
      }
    });
  }
}
