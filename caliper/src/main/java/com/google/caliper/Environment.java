/*
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

package com.google.caliper;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

/**
 * A description of an environment in which benchmarks are run.
 *
 * WARNING: a JSON representation of this class is stored on the app engine server. If any changes
 * are made to this class, a deserialization adapter must be written for this class to ensure
 * backwards compatibility.
 *
 * <p>Gwt-safe
 */
@SuppressWarnings("serial")
public final class Environment
    implements Serializable /* for GWT Serialization */ {
  private /*final*/ Map<String, String> propertyMap;

  public Environment(Map<String, String> propertyMap) {
    this.propertyMap = new HashMap<String, String>(propertyMap);
  }

  public Map<String, String> getProperties() {
    return propertyMap;
  }

  @Override public boolean equals(Object o) {
    return o instanceof Environment
        && ((Environment) o).propertyMap.equals(propertyMap);
  }

  @Override public int hashCode() {
    return propertyMap.hashCode();
  }

  @Override public String toString() {
    return propertyMap.toString();
  }

  @SuppressWarnings("unused")
  private Environment() {} // for GWT Serialization
}
