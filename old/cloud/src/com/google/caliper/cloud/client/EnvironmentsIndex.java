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

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class EnvironmentsIndex {
  private List<EnvironmentMeta> environments = null;
  private Collection<RunMeta> runMetas = new ArrayList<RunMeta>();

  public void invalidate() {
    environments = null;
  }

  public void setRunMetas(Collection<RunMeta> runMetas) {
    this.runMetas.clear();
    this.runMetas.addAll(runMetas);
    invalidate();
  }

  public void indexEnvironments() {
    Map<String, EnvironmentMeta> allEnvironments = new HashMap<String, EnvironmentMeta>();
    for (RunMeta run : runMetas) {
      if (run.getEnvironmentMeta() != null) {
        allEnvironments.put(run.getEnvironmentKey(), run.getEnvironmentMeta());
      }
    }
    environments = new ArrayList<EnvironmentMeta>(allEnvironments.values());
  }

  public List<EnvironmentMeta> getEnvironments() {
    if (environments == null) {
      indexEnvironments();
    }
    return environments;
  }
}
