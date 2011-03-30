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

package com.google.caliper.worker;

import com.google.gson.Gson;

/**
* @author Kevin Bourrillion
*/
public class Measurement {
  public static Measurement fromString(String json) {
    return new Gson().fromJson(json, Measurement.class);
  }

  public final long nanos;
  public final int reps;
  public final double nanosPerRep;

  public Measurement(long nanos, int reps) {
    this.nanos = nanos;
    this.reps = reps;
    this.nanosPerRep = ((double) nanos) / reps;
  }

  private Measurement() {
    nanos = 0;
    reps = 0;
    nanosPerRep = 0;
  }

  @Override public String toString() {
    return new Gson().toJson(this);
  }
}
