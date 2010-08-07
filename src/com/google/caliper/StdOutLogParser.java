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

public final class StdOutLogParser implements LogParser {

  private boolean inTimedSection;
  private boolean logLine;
  private boolean displayLine;
  private MeasurementSet measurementSet;

  String lineToDisplay;

  /**
   * Parse a single line of log output.
   *
   * [caliper] [starting scenarios]
   * [caliper] [starting warmup]
   * [GC 2914K->336K(184576K), 0.0021600 secs]
   * [Full GC 336K->274K(184576K), 0.0051340 secs]
   * [GC 274K->274K(184576K), 0.0008710 secs]
   * [Full GC 274K->274K(184576K), 0.0047980 secs]
   * ...
   * [caliper] [performing additional measurement with scale 1.00]
   * [caliper] [running trial with 730146 reps]
   * [GC 1263K->363K(184576K), 0.0006660 secs]
   * [Full GC 363K->300K(184576K), 0.0053820 secs]
   * [GC 300K->300K(184576K), 0.0002870 secs]
   * [Full GC 300K->299K(184576K), 0.0047650 secs]
   * [caliper] [starting timed section]
   * [caliper] [done timed section]
   * [caliper] [took 1361.90 nanoseconds per rep]
   * [caliper] [scenario finished] 1361.902022335259 1363.5176334596094 1363.857496993752 1369.5888351654407 1369.762495902646 1370.3947457083925 1372.1379614488062 1372.5909804340502 1418.7606560879606 1423.6248983080097
   * [caliper] [scenarios finished]
   */
  @Override public void readLine(String line) {
    boolean caliperLog = line.startsWith(LogConstants.CALIPER_LOG_PREFIX);
    // True if the line seems to indicate a garbage collection, like
    // [GC 1263K->363K(184576K), 0.0006660 secs]
    // or
    // [Full GC 363K->300K(184576K), 0.0053820 secs]
    boolean gcLog = line.startsWith("[GC ") || line.startsWith("[Full GC ");

    if (caliperLog) {
      String caliperLogLine = line.substring(LogConstants.CALIPER_LOG_PREFIX.length());

      // timed sections start with "[caliper] [starting timed section]"
      // end with "[caliper] [done timed section]"
      if (caliperLogLine.equals(LogConstants.TIMED_SECTION_STARTING)) {
        inTimedSection = true;
      } else if (caliperLogLine.equals(LogConstants.TIMED_SECTION_DONE)) {
        inTimedSection = false;
      }

      // get measurements from a line like this:
      // "[caliper] [scenario finished] 1361.902022335259 ..."
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
