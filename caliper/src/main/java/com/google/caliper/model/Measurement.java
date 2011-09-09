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

/**
 * A single numeric datum reported by an instrument for a particular scenario.
 */
public class Measurement {
  // For example, if 42 reps completed in 999000000 ns, then these values might be
  // (999000000.0, 42, "ns", "runtime")
  public double value;
  public double weight;
  public String unit;
  public String description;

  public static Measurement fromString(String json) {
    return ModelJson.fromJson(json, Measurement.class);
  }

  @Override public String toString() {
    return ModelJson.toJson(this);
  }
}
