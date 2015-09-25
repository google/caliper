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

import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.ui.Anchor;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.InlineLabel;
import com.google.gwt.user.client.ui.Widget;

public class RefreshButton implements ClickHandler {

  private final PageView view;
  private final FlowPanel panel;

  public RefreshButton(PageView view) {
    this.view = view;
    panel = new FlowPanel();
    panel.getElement().setAttribute("style", "display:inline;");
  }

  public Widget getWidget() {
    return panel;
  }

  public void show(String label) {
    Anchor refreshButton = new Anchor(label);
    refreshButton.addClickHandler(this);

    panel.clear();
    panel.add(refreshButton);
  }

  public void loading() {
    panel.clear();
    panel.add(new InlineLabel("Loading..."));
  }

  public void done() {
    show("Refresh");
  }

  public void failed() {
    show("Failed. Click to refresh.");
  }

  public void onClick(ClickEvent clickEvent) {
    view.reload(this);
  }
}
