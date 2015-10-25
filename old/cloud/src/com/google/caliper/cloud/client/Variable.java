/**
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


package com.google.caliper.cloud.client;

import com.google.caliper.Scenario;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;

public class Variable {
  private final String name;
  private final Map<String, Value> values = new LinkedHashMap<String, Value>();
  private final int keyIndex;

  public Variable(String name, int keyIndex) {
    this.name = name;
    this.keyIndex = keyIndex;
  }

  public final String getName() {
    return name;
  }

  public final int keyIndex() {
    return keyIndex;
  }

  /**
   * Ensures the given value exists for this variable.
   */
  public final Value addValue(String name) {
    Value value = values.get(name);
    if (value == null) {
      value = new Value(name);
      values.put(name, value);
    }
    return value;
  }

  public final void addValue(Value value) {
    Value previous = values.put(value.getName(), value);
    if (previous != null) {
      throw new IllegalStateException();
    }
  }

  public final Collection<Value> getValues() {
    return values.values();
  }

  /**
   * Returns the value with the specified name.
   */
  public final Value get(String valueName) {
    return values.get(valueName);
  }

  /**
   * Returns this variable's value as assigned to the given scenario and run.
   */
  public Value get(RunMeta runMeta, Scenario scenario) {
    String valueName = scenario.getVariables().get(name);
    return values.get(valueName);
  }

  public final Value getOnlyValue() {
    if (values.size() != 1) {
      throw new IllegalStateException("No values for " + name);
    }
    return values.values().iterator().next();
  }

  public final boolean hasMultipleValues() {
    return values.size() > 1;
  }

  @Override public String toString() {
    return name + ": " + values.keySet();
  }
}
