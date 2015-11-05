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
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Anchor;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.Widget;

public abstract class DeletedEditor<T extends Deletable> {

  protected final AsyncCallback<Void> deletionCallback = new AsyncCallback<Void>() {
    public void onFailure(Throwable throwable) {
      showDeleteButton();
    }

    public void onSuccess(Void unused) {
      deletable.setDeleted(true);
      showUndoButton();
    }
  };

  protected final AsyncCallback<Void> undeletionCallback = new AsyncCallback<Void>() {
    public void onFailure(Throwable throwable) {
      showUndoButton();
    }

    public void onSuccess(Void unused) {
      deletable.setDeleted(false);
      showDeleteButton();
    }
  };

  protected final T deletable;
  private final FlowPanel panel = new FlowPanel();

  public DeletedEditor(T deletable) {
    this.deletable = deletable;

    showDeleteButton();
  }

  protected abstract void persistDeletion();

  protected abstract void persistUndeletion();

  public Widget getWidget() {
    return panel;
  }

  private void showDeleteButton() {
    panel.clear();

    Anchor anchor = new Anchor("Delete");
    anchor.setStyleName("actionLink");
    anchor.addClickHandler(new ClickHandler() {
      public void onClick(ClickEvent clickEvent) {
        delete();
      }
    });
    panel.add(anchor);
  }

  public void delete() {
    panel.clear();
    panel.add(new Label("Deleting..."));

    persistDeletion();
  }

  private void showUndoButton() {
    panel.clear();

    Anchor anchor = new Anchor("Undo");
    anchor.setStyleName("actionLink");
    anchor.addClickHandler(new ClickHandler() {
      public void onClick(ClickEvent clickEvent) {
        undelete();
      }
    });
    panel.add(anchor);
  }

  public void undelete() {
    panel.clear();
    panel.add(new Label("Undeleting..."));

    persistUndeletion();
  }
}
