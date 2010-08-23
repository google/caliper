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

import com.google.caliper.LogEntry.LogEntryBuilder;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.io.BufferedReader;
import java.io.IOException;
import java.util.Map;

public final class SimpleLogParser implements LogParser {

  private final BufferedReader logReader;

  private boolean isDone;

  public SimpleLogParser(BufferedReader logReader) {
    this.logReader = logReader;
  }

  /**
   * Parse a single line of log output. This tries to assume the bar minimum about the format
   * of the output, and exists only as a fallback in case StdOutLogParser cannot be used.
   *
   * Expects at some point to receive a line like this, representing a scenario:
   *
   * [scenario] {"vm":"/usr/lib/jvm/java-6-sun/bin/java","benchmark":"AppendBoolean","length":"50"}
   *
   * where the string after "[scenario] " is a JSON string from which a scenario can be extracted.
   *
   * And a line like this, representing a measurement set:
   *
   * [measurement] {"measurements":[{"nanosPerRep":663.935830809371, ... }]}
   *
   * where the string after "[measurement] " is a JSON string from which a measurement set can be
   * extracted.
   */
  @Override public LogEntry getEntry() {
    if (isDone) {
      return null;
    }

    String line;
    try {
      line = logReader.readLine();
      if (line == null) {
        isDone = true;
        return null;
      }
    } catch (IOException e) {
      throw new RuntimeException("failed to read from log", e);
    }

    LogEntryBuilder logEntryBuilder = new LogEntryBuilder();

    boolean scenarioLog = line.startsWith(LogConstants.SCENARIO_JSON_PREFIX);
    boolean measurementLog = line.startsWith(LogConstants.MEASUREMENT_JSON_PREFIX);
    if (scenarioLog) {
      String scenarioString =
          line.substring(LogConstants.SCENARIO_JSON_PREFIX.length());
      logEntryBuilder.setScenario(new Scenario(new Gson().<Map<String, String>>fromJson(
          scenarioString, new TypeToken<Map<String, String>>() {}.getType())));
    } else if (measurementLog) {
      try {
        logEntryBuilder.setMeasurementSet(Json.measurementSetFromJson(
            line.substring(LogConstants.MEASUREMENT_JSON_PREFIX.length())));
      } catch (IllegalArgumentException ignore) {
      }
    }

    logEntryBuilder.setDisplayLine(line);
    logEntryBuilder.setLogLine(line);

    return logEntryBuilder.build();
  }
}
