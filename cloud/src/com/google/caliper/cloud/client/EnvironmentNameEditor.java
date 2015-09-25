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
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.*;

public class EnvironmentNameEditor extends NameEditor<EnvironmentMeta> {

  public EnvironmentNameEditor(EnvironmentMeta environment, Anchor editLink, Label nonEditLabel, boolean editable,
      AsyncCallback<Void> callback) {
    super(environment, editLink, nonEditLabel, editable, callback);
  }

  @Override public void persistName(String newName, AsyncCallback<Void> callback) {
    BenchmarkServiceAsync benchmarkService = GWT.create(BenchmarkService.class);
    benchmarkService.nameEnvironment(nameable.getId(), newName, callback);
  }
}