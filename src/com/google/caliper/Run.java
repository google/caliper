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

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import java.util.Map;

/**
 * A configured benchmark.
 */
public final class Run {

  static final String VM_KEY = "vm";

  /**
   * The subset of variable names that are managed by the system. It is an error
   * to create a parameter with the same name as one of these variables.
   */
  static final ImmutableSet<String> SYSTEM_VARIABLES = ImmutableSet.of(VM_KEY);

  private final ImmutableMap<String, String> variables;

  public Run(Map<String, String> variables) {
    this.variables = ImmutableMap.copyOf(variables);
  }

  public ImmutableMap<String, String> getVariables() {
    return variables;
  }

  /**
   * Returns the user-specified parameters. This is the (possibly-empty) set of
   * variables that may be varied from run to run in the same environment.
   */
  public ImmutableMap<String, String> getParameters() {
    ImmutableMap.Builder<String, String> builder = ImmutableMap.builder();
    for (Map.Entry<String, String> entry : variables.entrySet()) {
      if (!SYSTEM_VARIABLES.contains(entry.getKey())) {
        builder.put(entry.getKey(), entry.getValue());
      }
    }
    return builder.build();
  }

  @Override public boolean equals(Object o) {
    return o instanceof Run
        && ((Run) o).getVariables().equals(variables);
  }

  @Override public int hashCode() {
    return variables.hashCode();
  }

  @Override public String toString() {
    return "Run" + variables;
  }
}
