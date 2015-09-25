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

import com.google.gwt.core.client.EntryPoint;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.RootPanel;

public class Caliper implements EntryPoint {

  private RefreshButton refreshButton;

  private PageView view;

  public void onModuleLoad() {
    String path = Window.Location.getPath();
    if (path.startsWith("/run/")) {
      view = new BenchmarkView();
      view.init();
    } else if (path.startsWith("/user/")) {
      view = new UserDashboardView();
      view.init();
    }
    refreshButton = new RefreshButton(view);
    RootPanel.get("refresh").add(refreshButton.getWidget());
    reload();
  }

  private void reload() {
    view.reload(refreshButton);
  }
}
