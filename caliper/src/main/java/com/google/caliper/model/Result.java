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

import com.google.caliper.util.Util;

import java.util.List;
import java.util.SortedMap;

/**
 * A set of measurements, optionally including arbitrary report text, that were taken by a
 * particular instrument for a particular scenario.
 */
public class Result {
  public String localName;

  public String scenarioLocalName;
  public String instrumentLocalName;

  public List<String> vmCommandLine;

  public List<Measurement> measurements;
  public String reportInfo;

  public static Result fromString(String json) {
    return Util.GSON.fromJson(json, Result.class);
  }

  @Override public String toString() {
    return Util.GSON.toJson(this);
  }
}
