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

public final class StdOutLogProcessor implements LogProcessor {

  private boolean inTimedSection;
  private boolean logLine;
  private boolean displayLine;
  private MeasurementSet measurementSet;

  String lineToDisplay;

  @Override public void readLine(String line) {
    boolean caliperLog = line.startsWith(LogConstants.CALIPER_LOG_PREFIX);
    boolean gcLog = line.startsWith("[GC ") || line.startsWith("[Full GC ");

    if (caliperLog) {
      String caliperLogLine = line.substring(LogConstants.CALIPER_LOG_PREFIX.length());

      if (caliperLogLine.equals(LogConstants.TIMED_SECTION_STARTING)) {
        inTimedSection = true;
      } else if (caliperLogLine.equals(LogConstants.TIMED_SECTION_DONE)) {
        inTimedSection = false;
      }

      if (caliperLogLine.startsWith(LogConstants.MEASUREMENT_PREFIX)) {
        try {
          measurementSet = MeasurementSet.valueOf(
              caliperLogLine.substring(LogConstants.MEASUREMENT_PREFIX.length()));
        } catch (IllegalArgumentException ignore) {
        }
      }
    }

    logLine = caliperLog || inTimedSection;
    displayLine = !caliperLog && !gcLog;
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

  @Override public boolean isLogDone() {
    // When reading from stdout, the log will naturally end, and doesn't need the log
    // manager to tell it to terminate, so this should always return false.
    return false;
  }
}
