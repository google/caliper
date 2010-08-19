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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class AdbLogParser implements LogParser {

  private boolean startLogging;
  private boolean inTimedSection;
  private boolean logLine;
  private boolean displayLine;
  private MeasurementSet measurementSet;
  private Scenario scenario;
  private boolean isLogDone;
  private int processId;

  String lineToLog;
  String lineToDisplay;

  /**
   * Extracts a pid from a line of log output that looks approximately like this:
   * I/stdout  (19051): [caliper] [starting scenarios]
   *            ^^^^^
   */
  private int getProcessId(String line) {
    Pattern processIdPattern = Pattern.compile(".*\\((\\d+)\\):.*");
    Matcher matcher = processIdPattern.matcher(line);
    if (matcher.matches()) {
      return Integer.valueOf(matcher.group(1));
    }
    return 0;
  }

  /**
   * Parse a single line of log output.
   *
   * Expects input like this sample:
   *
   * --------- beginning of /dev/log/main
   * D/dalvikvm(19051): creating instr width table
   * D/dalvikvm(19051): mprotect(RO) failed (13), file will remain read-write
   * D/dalvikvm(19051): mprotect(RO) failed (13), file will remain read-write
   * D/dalvikvm(19051): mprotect(RO) failed (13), file will remain read-write
   * D/dalvikvm(19051): mprotect(RO) failed (13), file will remain read-write
   * D/dalvikvm(19051): mprotect(RO) failed (13), file will remain read-write
   * D/dalvikvm(19051): mprotect(RO) failed (13), file will remain read-write
   * D/dalvikvm(19051): mprotect(RO) failed (13), file will remain read-write
   * D/dalvikvm(19051): mprotect(RO) failed (13), file will remain read-write
   * I/stdout  (19051): [caliper] [starting scenarios]
   * I/stdout  (19051): [caliper] [starting warmup]
   * D/dalvikvm(19051): GC_EXPLICIT freed 1334 objects / 86264 bytes in 2ms
   * D/dalvikvm(19051): GC_EXPLICIT freed 4 objects / 168 bytes in 5ms
   * I/stdout  (19051): [caliper] [starting timed section]
   * I/stdout  (19051): [caliper] [done timed section]
   * D/dalvikvm(19051): GC_EXPLICIT freed 55 objects / 34808 bytes in 6ms
   * D/dalvikvm(19051): GC_EXPLICIT freed 0 objects / 0 bytes in 2ms
   * I/stdout  (19051): [caliper] [starting timed section]
   * I/stdout  (19051): [caliper] [done timed section]
   * ...
   * D/dalvikvm(19051): GC_EXPLICIT freed 57 objects / 34832 bytes in 2ms
   * D/dalvikvm(19051): GC_EXPLICIT freed 0 objects / 0 bytes in 2ms
   * I/stdout  (19051): [caliper] [starting timed section]
   * I/stdout  (19051): [caliper] [done timed section]
   * I/stdout  (19051): [caliper] [ending warmup]
   * I/stdout  (19051): [caliper] [starting scenario] Scenario{vm=dalvikvm, benchmark=ArrayCopyManual}
   * I/stdout  (19051): [caliper] [measuring nanos per rep with scale 1.00]
   * I/stdout  (19051): [caliper] [running trial with 6529 reps]
   * D/dalvikvm(19051): GC_EXPLICIT freed 109 objects / 37344 bytes in 7ms
   * D/dalvikvm(19051): GC_EXPLICIT freed 0 objects / 0 bytes in 5ms
   * I/stdout  (19051): [caliper] [starting timed section]
   * I/stdout  (19051): [caliper] [done timed section]
   * I/stdout  (19051): [caliper] [took 154102.32 nanoseconds per rep]
   * I/stdout  (19051): [caliper] [measuring nanos per rep with scale 0.50]
   * I/stdout  (19051): [caliper] [running trial with 3244 reps]
   * D/dalvikvm(19051): GC_EXPLICIT freed 191 objects / 39704 bytes in 7ms
   * D/dalvikvm(19051): GC_EXPLICIT freed 16 objects / 688 bytes in 2ms
   * I/stdout  (19051): [caliper] [starting timed section]
   * I/stdout  (19051): [caliper] [done timed section]
   * I/stdout  (19051): [caliper] [took 152587.89 nanoseconds per rep]
   * I/stdout  (19051): [caliper] [measuring nanos per rep with scale 1.50]
   * I/stdout  (19051): [caliper] [running trial with 9733 reps]
   * D/dalvikvm(19051): GC_EXPLICIT freed 111 objects / 37240 bytes in 7ms
   * D/dalvikvm(19051): GC_EXPLICIT freed 4 objects / 160 bytes in 6ms
   * I/stdout  (19051): [caliper] [starting timed section]
   * I/stdout  (19051): [caliper] [done timed section]
   * I/stdout  (19051): [caliper] [took 152048.59 nanoseconds per rep]
   * I/stdout  (19051): [caliper] [scenario finished] 152048.58882153497 152587.890567201 154102.31781283504
   * I/stdout  (19051): [caliper] [scenarios finished]
   */
  @Override public void readLine(String line) {
    boolean isThisProcess = getProcessId(line) == processId;

    // strip off log stuff (e.g. "I/stdout  (19051): ...")
    int messageStart = line.indexOf(':');
    String normalizedLine = line.substring(messageStart + 2);

    boolean isScenarioLog = normalizedLine.startsWith(LogConstants.SCENARIO_XML_PREFIX);
    // true if this line indicates a garbage collection
    // e.g., "GC_EXPLICIT freed 191 objects / 39704 bytes in 7ms"
    boolean isGcLog = normalizedLine.startsWith("GC_");
    boolean isCaliperLog = normalizedLine.startsWith(LogConstants.CALIPER_LOG_PREFIX);

    if (isScenarioLog) {
      String scenarioString =
          normalizedLine.substring(LogConstants.SCENARIO_XML_PREFIX.length());
      ByteArrayInputStream scenarioXml = new ByteArrayInputStream(scenarioString.getBytes());
      Properties properties = new Properties();
      try {
        properties.loadFromXML(scenarioXml);
        scenario = new Scenario(Maps.fromProperties(properties));
        scenarioXml.close();
      } catch (IOException e) {
        throw new RuntimeException("failed to load properties from xml " + scenarioString, e);
      }
    } else if (isCaliperLog) {
      String caliperLogLine =
          normalizedLine.substring(LogConstants.CALIPER_LOG_PREFIX.length());

      // start logging when you see a line like "I/stdout  (19051): [caliper] [starting scenarios]"
      // stop when you see a line like "I/stdout  (19051): [caliper] [scenarios finished]"
      if (caliperLogLine.equals(LogConstants.SCENARIOS_STARTING)) {
        startLogging = true;
        processId = getProcessId(line);
      } else if (caliperLogLine.equals(LogConstants.SCENARIOS_FINISHED)) {
        isLogDone = true;
      }

      // timed sections start with "I/stdout  (19051): [caliper] [starting timed section]"
      // end with "I/stdout  (19051): [caliper] [done timed section]"
      if (caliperLogLine.equals(LogConstants.TIMED_SECTION_STARTING)) {
        inTimedSection = true;
      } else if (caliperLogLine.equals(LogConstants.TIMED_SECTION_DONE)) {
        inTimedSection = false;
      }

      // get measurements from a line like this:
      // "I/stdout  (19051): [caliper] [scenario finished] 152048.58882153497 ..."
      if (caliperLogLine.startsWith(LogConstants.MEASUREMENT_PREFIX)) {
        try {
          measurementSet = Json.measurementSetFromJson(
              caliperLogLine.substring(LogConstants.MEASUREMENT_PREFIX.length()));
        } catch (IllegalArgumentException ignore) {
        }
      }
    }

    logLine = startLogging && (isCaliperLog || inTimedSection);
    displayLine = isThisProcess && startLogging && !isCaliperLog && !isGcLog && !isScenarioLog;
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

  @Override public Scenario getScenario() {
    return scenario;
  }

  @Override public boolean isLogDone() {
    return isLogDone;
  }
}
