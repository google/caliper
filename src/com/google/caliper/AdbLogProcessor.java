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

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class AdbLogProcessor implements LogProcessor {

  private boolean startLogging;
  private boolean inTimedSection;
  private boolean logLine;
  private boolean displayLine;
  private MeasurementSet measurementSet;
  private boolean isLogDone;
  private int processId;

  String lineToLog;
  String lineToDisplay;

  private int getProcessId(String line) {
    Pattern processIdPattern = Pattern.compile(".*\\((\\d+)\\):.*");
    Matcher matcher = processIdPattern.matcher(line);
    if (matcher.matches()) {
      return Integer.valueOf(matcher.group(1));
    }
    return 0;
  }

  @Override public void readLine(String line) {
    boolean isThisProcess = getProcessId(line) == processId;

    // strip off log stuff
    int messageStart = line.indexOf(':');
    String normalizedLine = line.substring(messageStart + 2);

    boolean isGcLog = normalizedLine.startsWith("GC_");
    boolean isCaliperLog = normalizedLine.startsWith(LogConstants.CALIPER_LOG_PREFIX);

    if (isCaliperLog) {
      String caliperLogLine =
          normalizedLine.substring(LogConstants.CALIPER_LOG_PREFIX.length());

      if (caliperLogLine.equals(LogConstants.SCENARIOS_STARTING)) {
        startLogging = true;
        processId = getProcessId(line);
      } else if (caliperLogLine.equals(LogConstants.SCENARIOS_FINISHED)) {
        isLogDone = true;
      }

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

    logLine = startLogging && (isCaliperLog || inTimedSection);
    displayLine = isThisProcess && startLogging && !isCaliperLog && !isGcLog;
    lineToLog = line;
    lineToDisplay = normalizedLine;
  }

  @Override public String lineToLog() {
    return lineToLog;
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
    return isLogDone;
  }
}
