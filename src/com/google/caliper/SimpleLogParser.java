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

import com.google.common.collect.Maps;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Properties;

public final class SimpleLogParser implements LogParser {
  private MeasurementSet measurementSet;

  private boolean logLine = true;
  private boolean displayLine = true;
  private Scenario scenario;

  private String lineToDisplay;

  @Override public void readLine(String line) {
    boolean scenarioLog = line.startsWith(LogConstants.SCENARIO_XML_PREFIX);
    if (scenarioLog) {
      String scenarioString =
          line.substring(LogConstants.SCENARIO_XML_PREFIX.length());
      ByteArrayInputStream scenarioXml = new ByteArrayInputStream(scenarioString.getBytes());
      Properties properties = new Properties();
      try {
        properties.loadFromXML(scenarioXml);
        scenario = new Scenario(Maps.fromProperties(properties));
        scenarioXml.close();
      } catch (IOException e) {
        throw new RuntimeException("failed to load properties from xml", e);
      }
    }

    if (line.startsWith(LogConstants.MEASUREMENT_PREFIX)) {
      try {
        measurementSet =
            MeasurementSet.valueOf(line.substring(LogConstants.MEASUREMENT_PREFIX.length()));
      } catch (IllegalArgumentException ignore) {
      }
    }

    lineToDisplay = line;
  }

  @Override public String lineToLog() {
    return lineToDisplay;
  }

  @Override public String lineToDisplay() {
    return lineToDisplay;
  }

  @Override public boolean logLine() {
    return logLine;
  }

  @Override public boolean displayLine() {
    return displayLine;
  }

  @Override public MeasurementSet getMeasurementSet() {
    return measurementSet;
  }

  @Override public Scenario getScenario() {
    return scenario;
  }

  @Override public boolean isLogDone() {
    // When reading from stdout, the log will naturally end, and doesn't need to log
    // manager to tell it to terminate, so this should always return false.
    return false;
  }
}
