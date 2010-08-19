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

import com.google.common.collect.ImmutableMap;
import com.google.gson.Gson;
import com.google.gson.JsonParseException;
import com.google.gson.reflect.TypeToken;
import java.util.ArrayList;
import java.util.List;

/**
 * Ordinarily serialization should be done within the class that is being serialized. However,
 * many of these classes are used by GWT, which dies when it sees Gson.
 */
public final class Json {
  public static String measurementSetToJson(MeasurementSet measurementSet) {
    return new Gson().toJson(measurementSet);
  }

  /**
   * Attempts to extract a MeasurementSet from a string, assuming it is JSON. If this fails, it
   * tries to extract it from the string assuming it is a space-separated list of double values.
   */
  public static MeasurementSet measurementSetFromJson(String measurementSetJson) {
    try {
      return new Gson().fromJson(measurementSetJson, new TypeToken<MeasurementSet>() {}.getType());
    } catch (JsonParseException e) {
      // might be an old MeasurementSet, so fall back on failure to the old, space separated
      // serialization method.
      try {
        String[] measurementStrings = measurementSetJson.split("\\s+");
        List<Measurement> measurements = new ArrayList<Measurement>();
        for (String s : measurementStrings) {
          measurements.add(
              new Measurement(ImmutableMap.of("ns", 1, "us", 1000, "ms", 1000000, "s", 1000000000),
              Double.valueOf(s), Double.valueOf(s)));
        }
        // seconds and variations is the default unit
        return new MeasurementSet(measurements.toArray(new Measurement[measurements.size()]));
      } catch (NumberFormatException ignore) {
        throw new IllegalArgumentException("Not a measurement set: " + measurementSetJson);
      }
    }
  }

  private Json() {}
}