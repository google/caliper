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
import com.google.gson.GsonBuilder;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import com.google.gson.reflect.TypeToken;
import java.lang.reflect.Type;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;

/**
 * Ordinarily serialization should be done within the class that is being serialized. However,
 * many of these classes are used by GWT, which dies when it sees Gson.
 */
public final class Json {
  /**
   * This Gson instance must be used when serializing a class that includes a Run as a member
   * or as a member of a member (etc.), otherwise the Map<Scenario, ScenarioResult> will not
   * be correctly serialized.
   */
  private static final Gson GSON_INSTANCE =
      new GsonBuilder()
          .registerTypeAdapter(Date.class, new DateTypeAdapter())
          .registerTypeAdapter(Run.class, new RunTypeAdapter())
          .registerTypeAdapter(Measurement.class, new MeasurementDeserializer())
          .create();

  public static Gson getGsonInstance() {
    return GSON_INSTANCE;
  }

  public static String measurementSetToJson(MeasurementSet measurementSet) {
    return new Gson().toJson(measurementSet);
  }

  /**
   * Attempts to extract a MeasurementSet from a string, assuming it is JSON. If this fails, it
   * tries to extract it from the string assuming it is a space-separated list of double values.
   */
  public static MeasurementSet measurementSetFromJson(String measurementSetJson) {
    try {
      return getGsonInstance().fromJson(measurementSetJson, MeasurementSet.class);
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

  public static MeasurementSet measurementSetFromJson(JsonObject measurementSetJson) {
    return getGsonInstance().fromJson(measurementSetJson, MeasurementSet.class);
  }

  /**
   * Backwards compatibility!
   */
  private static class MeasurementDeserializer implements JsonDeserializer<Measurement> {
    @Override public Measurement deserialize(JsonElement jsonElement, Type type,
        JsonDeserializationContext context) throws JsonParseException {
      JsonObject obj = jsonElement.getAsJsonObject();
      if (obj.has("raw") && obj.has("processed")) {
        return new Measurement(
            context.<Map<String, Integer>>deserialize(obj.get("unitNames"),
                new TypeToken<Map<String, Integer>>() {}.getType()),
            context.<Double>deserialize(obj.get("raw"), Double.class),
            context.<Double>deserialize(obj.get("processed"), Double.class));
      }
      if (obj.has("nanosPerRep") && obj.has("unitsPerRep") && obj.has("unitNames")) {
        return new Measurement(
            context.<Map<String, Integer>>deserialize(obj.get("unitNames"),
                new TypeToken<Map<String, Integer>>() {}.getType()),
            context.<Double>deserialize(obj.get("nanosPerRep"), Double.class),
            context.<Double>deserialize(obj.get("unitsPerRep"), Double.class));
      }
      throw new JsonParseException(obj.toString());
    }
  }

  /**
   * This adapter is necessary because gson doesn't handle Maps more complex than Map<String, ...>
   * in a useful way. For example, Map<Scenario, ScenarioResult>'s serialized version simply uses
   * Scenario.toString() as the keys. This adapter stores this Map as lists of
   * KeyValuePair<Scenario, ScenarioResult> instead, to preserve the Scenario objects on
   * deserialization.
   */
  private static class RunTypeAdapter implements JsonSerializer<Run>, JsonDeserializer<Run> {

    @Override public Run deserialize(JsonElement jsonElement, Type type,
        JsonDeserializationContext context) throws JsonParseException {

      List<KeyValuePair<Scenario, ScenarioResult>> mapList = context.deserialize(
          jsonElement.getAsJsonObject().get("measurements"),
          new TypeToken<List<KeyValuePair<Scenario, ScenarioResult>>>() {}.getType());
      Map<Scenario, ScenarioResult> measurements = new LinkedHashMap<Scenario, ScenarioResult>();
      for (KeyValuePair<Scenario, ScenarioResult> entry : mapList) {
        measurements.put(entry.getKey(), entry.getValue());
      }

      String benchmarkName =
          context.deserialize(jsonElement.getAsJsonObject().get("benchmarkName"), String.class);

      Date executedTimestamp = context.deserialize(
          jsonElement.getAsJsonObject().get("executedTimestamp"), Date.class);

      return new Run(measurements, benchmarkName, executedTimestamp);
    }

    @Override public JsonElement serialize(Run run, Type type, JsonSerializationContext context) {
      JsonObject result = new JsonObject();
      result.add("benchmarkName", context.serialize(run.getBenchmarkName()));
      result.add("executedTimestamp", context.serialize(run.getExecutedTimestamp()));

      List<KeyValuePair<Scenario, ScenarioResult>> mapList =
          new ArrayList<KeyValuePair<Scenario, ScenarioResult>>();
      for (Map.Entry<Scenario, ScenarioResult> entry : run.getMeasurements().entrySet()) {
        mapList.add(new KeyValuePair<Scenario, ScenarioResult>(entry.getKey(), entry.getValue()));
      }
      result.add("measurements", context.serialize(mapList,
          new TypeToken<List<KeyValuePair<Scenario, ScenarioResult>>>() {}.getType()));

      return result;
    }
  }

  private static class DateTypeAdapter implements JsonSerializer<Date>, JsonDeserializer<Date> {
    private final DateFormat dateFormat;

    private DateTypeAdapter() {
      dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssz", Locale.US);
      dateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
    }

    @Override public synchronized JsonElement serialize(Date date, Type type,
        JsonSerializationContext jsonSerializationContext) {
      return new JsonPrimitive(dateFormat.format(date));
    }

    @Override public synchronized Date deserialize(JsonElement jsonElement, Type type,
        JsonDeserializationContext jsonDeserializationContext) {
      String dateString = jsonElement.getAsString();
      // first try to parse as an ISO 8601 date
      try {
        return dateFormat.parse(dateString);
      } catch (ParseException ignored) {
      }
      // next, try a GSON-style locale-specific dates (for Caliper r282 and earlier)
      try {
        return DateFormat.getDateTimeInstance().parse(dateString);
      } catch (ParseException ignored) {
      }
      throw new JsonParseException(dateString);
    }
  }

  /**
   * This is similar to the Map.Entry class, but is necessary since Entrys are not supported
   * by gson.
   */
  private static class KeyValuePair<K, V> {
    private K k;
    private V v;

    KeyValuePair(K k, V v) {
      this.k = k;
      this.v = v;
    }

    public K getKey() {
      return k;
    }

    public V getValue() {
      return v;
    }

    private KeyValuePair() {} // for gson
  }

  private Json() {} // static class
}
