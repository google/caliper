/*
 * Copyright (C) 2009 Google Inc.
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

package com.google.caliper;

import java.io.Serializable;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/**
 * A configured benchmark.
 *
 * WARNING: a JSON representation of this class is stored on the app engine server. If any changes
 * are made to this class, a deserialization adapter must be written for this class to ensure
 * backwards compatibility.
 *
 * <p>Gwt-safe.
 */
@SuppressWarnings("serial")
public final class Scenario
    implements Serializable /* for GWT */  {

  static final String VM_KEY = "vm";

  /**
   * The subset of variable names that are managed by the system. It is an error
   * to create a parameter with the same name as one of these variables.
   */
  static final Set<String> SYSTEM_VARIABLES = new HashSet<String>(Arrays.asList(VM_KEY));

  private /*final*/ Map<String, String> variables;

  public Scenario(Map<String, String> variables) {
    this.variables = new LinkedHashMap<String, String>(variables);
  }

  public Map<String, String> getVariables() {
    return variables;
  }

  /**
   * Returns the user-specified parameters. This is the (possibly-empty) set of
   * variables that may be varied from scenario to scenario in the same
   * environment.
   */
  public Map<String, String> getParameters() {
    Map<String, String> result = new LinkedHashMap<String, String>();
    for (Map.Entry<String, String> entry : variables.entrySet()) {
      if (!SYSTEM_VARIABLES.contains(entry.getKey())) {
        result.put(entry.getKey(), entry.getValue());
      }
    }
    return result;
  }

  @Override public boolean equals(Object o) {
    return o instanceof Scenario
        && ((Scenario) o).getVariables().equals(variables);
  }

  @Override public int hashCode() {
    return variables.hashCode();
  }

  @Override public String toString() {
    return "Scenario" + variables;
  }

  private Scenario() {} // for GWT
}
