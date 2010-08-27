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
import java.io.BufferedReader;
import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class AndroidLogParser implements LogParser {

  private final BufferedReader logReader;
  
  private boolean startLogging;
  private boolean inTimedSection;
  private int processId = -1;
  private boolean isDone;

  public AndroidLogParser(BufferedReader logReader) {
    this.logReader = logReader;
  }

  /**
   * Parse a chunk of log output and return a LogEntry representing the information for that chunk.
   *
   * A chunk is usually one line, but occasionally extends for more than one line if the
   * log wraps a line.
   *
   * Expects input like this sample (note that long lines may be wrapped. Lines that are wrapped
   * are ended with a !, and the line continues on the next line. Measurement set JSON output
   * is often wrapped.):
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
   * I/stdout  (22476): [scenario] {"vm":"dalvikvm","benchmark":"ArrayCopyManual"}
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
   * I/stdout  (19051): [measurement] {"measurements":[{"timeUnitNames":{"us":1000,"ns":1,"s":1000000000,"ms":1000000},"nanosPerRep":20.40590815869826,...
   * I/stdout  (19051): [caliper] [scenario finished]
   * I/stdout  (19051): [caliper] [scenarios finished]
   */
  @Override public LogEntry getEntry() {
    if (isDone) {
      return null;
    }

    boolean isThisProcess = false;

    StringBuilder normalizedLineBuilder = new StringBuilder();
    StringBuilder lineBuilder = new StringBuilder();
    boolean isFirstLine = true;
    boolean isLineFinished = false;
    while (!isLineFinished) {
      String line;
      try {
        line = logReader.readLine();
        if (line == null) {
          System.out.println("logReader line is null");
          isDone = true;
          return null;
        }
      } catch (IOException e) {
        throw new RuntimeException("failed to read from android log", e);
      }

      if (isFirstLine) {
        isThisProcess = (processId != -1 && getProcessId(line) == processId);
      }

      // strip off log stuff (e.g. "I/stdout  (19051): ...")
      int messageStart = line.indexOf(':');
      String normalizedLine = line.substring(messageStart + 2);

      // wrapped lines end with "!", so try to grab another line and don't set isLineFinished
      if (normalizedLine.endsWith("!")) {
        if (isFirstLine) {
          lineBuilder.append(line.substring(0, line.length() - 1));
        } else {
          lineBuilder.append(normalizedLine.substring(0, normalizedLine.length() - 1));
        }
        normalizedLineBuilder.append(normalizedLine.substring(0, normalizedLine.length() - 1));
      } else {
        if (isFirstLine) {
          lineBuilder.append(line);
        } else {
          lineBuilder.append(normalizedLine);
        }
        normalizedLineBuilder.append(normalizedLine);
        isLineFinished = true;
      }
      isFirstLine = false;
    }
    String line = lineBuilder.toString();
    String normalizedLine = normalizedLineBuilder.toString();

    boolean isMeasurementLog = normalizedLine.startsWith(LogConstants.MEASUREMENT_JSON_PREFIX);
    // true if this line indicates a garbage collection
    // e.g., "GC_EXPLICIT freed 191 objects / 39704 bytes in 7ms"
    boolean isGcLog = normalizedLine.startsWith("GC_");
    boolean isCaliperLog = normalizedLine.startsWith(LogConstants.CALIPER_LOG_PREFIX);

    LogEntryBuilder logEntryBuilder = new LogEntryBuilder();

    if (isMeasurementLog) {
      try {
        logEntryBuilder.setMeasurementSet(Json.measurementSetFromJson(
            normalizedLine.substring(LogConstants.MEASUREMENT_JSON_PREFIX.length())));
      } catch (IllegalArgumentException ignore) {
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
        isDone = true;
      }

      // timed sections start with "I/stdout  (19051): [caliper] [starting timed section]"
      // end with "I/stdout  (19051): [caliper] [done timed section]"
      if (caliperLogLine.equals(LogConstants.MEASURED_SECTION_STARTING)) {
        inTimedSection = true;
      } else if (caliperLogLine.equals(LogConstants.MEASURED_SECTION_DONE)) {
        inTimedSection = false;
      }
    }

    if (startLogging && (isCaliperLog || inTimedSection)) {
      logEntryBuilder.setLogLine(line);
    }
    if (isThisProcess && startLogging && !isCaliperLog && !isGcLog) {
      logEntryBuilder.setDisplayLine(normalizedLine);
    }

    return logEntryBuilder.build();
  }

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
}
