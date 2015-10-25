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
import com.google.gwt.event.dom.client.KeyPressEvent;
import com.google.gwt.event.dom.client.KeyPressHandler;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Anchor;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.user.client.ui.Widget;

public abstract class NameEditor<T extends Nameable> implements ClickHandler, KeyPressHandler,
    AsyncCallback<Void> {

  protected final T nameable;
  private final Anchor editLink;
  private final Label nonEditLabel;
  private final boolean editable;
  private final AsyncCallback<Void> callback;

  private final FlowPanel panel = new FlowPanel();

  private TextBox textBox;
  private String nameCandidate;

  /**
   * The provided callback's onSuccess method is called after the {@code nameable}'s name has been
   * updated, and its onFailure method is called after any cleanup is done.
   */
  public NameEditor(T nameable, Anchor editLink, Label nonEditLabel, boolean editable,
      AsyncCallback<Void> callback) {
    this.nameable = nameable;
    this.editLink = editLink;
    this.editLink.addClickHandler(this);
    this.nonEditLabel = nonEditLabel;
    this.editable = editable;
    this.callback = callback;

    showLink();
  }

  public abstract void persistName(String newName, AsyncCallback<Void> callback);

  public Widget getWidget() {
    return panel;
  }

  private void showLink() {
    panel.clear();
    if (editable) {
      editLink.setText(nameable.getName());
      panel.add(editLink);
    } else {
      nonEditLabel.setText(nameable.getName());
      panel.add(nonEditLabel);
    }
  }

  private void showEditor(String initialText) {
    panel.clear();

    // build the text box and save button
    textBox = new TextBox();
    textBox.setText(initialText);

    panel.add(textBox);
    panel.add(new Button("Save", this));

    textBox.setFocus(true);
    textBox.selectAll();

    textBox.addKeyPressHandler(this);
  }

  private void showSaving() {
    panel.clear();
    panel.add(new Label("Saving..."));
  }

  private void saveRename() {
    showSaving();
    nameCandidate = textBox.getText();
    persistName(nameCandidate, this);
  }

  private void cancelRename() {
    showLink();
  }

  public void onClick(ClickEvent event) {
    if (event.getSource() instanceof Anchor) {
      showEditor(nameable.getName());
    } else if (event.getSource() instanceof Button) {
      saveRename();
    }
  }

  public void onKeyPress(KeyPressEvent event) {
    if (event.getCharCode() == '\r') {
      saveRename();
    } else if (event.getCharCode() == 27) { // escape
      cancelRename();
    }
  }

  public void onFailure(Throwable throwable) {
    showEditor(nameCandidate);
    callback.onFailure(throwable);
  }

  public void onSuccess(Void unused) {
    nameable.setName(nameCandidate);
    showLink();
    callback.onSuccess(unused);
  }
}
