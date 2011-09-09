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

import java.util.SortedMap;

/**
 * The details of what kind of measurement was taken and how; three examples of instruments are
 * "the memory-allocation instrument with default settings", "the microbenchmark instrument with
 * default settings," and "the microbenchmark instrument with warmup time 2 seconds and timing
 * interval 0.5 seconds".
 */
public class Instrument {
  public String localName;
  public String className;

  public SortedMap<String, String> properties;

  public static Instrument fromString(String json) {
    return ModelJson.fromJson(json, Instrument.class);
  }

  @Override public String toString() {
    return ModelJson.toJson(this);
  }
}
