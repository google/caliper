/*
 * Copyright (C) 2011 Google Inc.
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

package com.google.caliper.model;

import java.util.LinkedHashMap;
import java.util.SortedMap;
import java.util.TreeMap;

/**
 * The collected information that caliper detects about the hardware and operating system it is
 * running under.
 */
public class Environment {
  public String localName;

  public TreeMap<String, String> properties = new TreeMap<String, String>();

  public static Environment fromString(String json) {
    return ModelJson.fromJson(json, Environment.class);
  }

  @Override public String toString() {
    return ModelJson.toJson(this);
  }
}
