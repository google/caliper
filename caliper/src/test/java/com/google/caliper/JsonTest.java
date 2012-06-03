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

package com.google.caliper;

import com.google.common.collect.ImmutableMap;

import java.util.Date;
import java.util.Locale;
import java.util.Map;
import junit.framework.TestCase;

public final class JsonTest extends TestCase {


  public void testJsonSerialization() {
    Result original = newSampleResult();
    String json = Json.getGsonInstance().toJson(original, Result.class);
    Result reserialized = Json.getGsonInstance().fromJson(json, Result.class);
    assertEquals(original, reserialized);
  }

  /**
   * Caliper's JSON files used to include dates specific to the host machine's
   * locale. http://code.google.com/p/caliper/issues/detail?id=113
   */

  public void testJsonSerializationWithFancyLocale() {
    Result original = newSampleResult();

    // serialize in one locale...
    Locale defaultLocale = Locale.getDefault();
    Locale.setDefault(Locale.ITALY);
    String json;
    try {
      json = Json.getGsonInstance().toJson(original, Result.class);
    } finally {
      Locale.setDefault(defaultLocale);
    }

    // deserialize in another
    Result reserialized = Json.getGsonInstance().fromJson(json, Result.class);
    assertEquals(original, reserialized);
  }

  private Result newSampleResult() {
    Map<String,Integer> units = ImmutableMap.of("ns", 1);
    MeasurementSet timeMeasurements = new MeasurementSet(new Measurement(units, 2.0, 2.0));
    Date executedDate = new Date(0);
    Scenario scenario = new Scenario(ImmutableMap.of("benchmark", "Foo"));
    ScenarioResult scenarioResult = new ScenarioResult(
        timeMeasurements, "log", null, null, null, null);
    Run run = new Run(ImmutableMap.of(scenario, scenarioResult), "foo.FooBenchmark", executedDate);
    Environment environment = new Environment(ImmutableMap.of("os.name", "Linux"));
    return new Result(run, environment);
  }
}
