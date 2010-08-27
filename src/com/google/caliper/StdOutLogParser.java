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
import java.util.regex.Pattern;

public final class StdOutLogParser implements LogParser {

  // For -XX:+PrintCompilation integration. Designed to match stuff like:
  //   1%  b   benchmarks.regression.StringBuilderBenchmark::timeAppendBoolean @ 18 (46 bytes)
  //  14   b   benchmarks.regression.StringBuilderBenchmark::timeAppendBoolean (46 bytes)
  private static final Pattern compilationPattern =
      Pattern.compile(".*\\(\\d+\\s+bytes\\)$|^---.*");

  private final BufferedReader logReader;

  private boolean inTimedSection;
  private boolean isDone;

  public StdOutLogParser(BufferedReader logReader) {
    this.logReader = logReader;
  }

  /**
   * Parse a single line of log output. The logs should look something like this:
   *
   *   1   b   java.lang.String::indexOf (151 bytes)
   *   2   b   java.lang.String::lastIndexOf (156 bytes)
   *   3   b   java.lang.String::hashCode (60 bytes)
   *   4   b   java.lang.String::replace (142 bytes)
   * ...
   *   8   b   java.lang.String::equals (88 bytes)
   * [caliper] [starting scenarios]
   * [scenario] {"vm":"/usr/lib/jvm/java-6-sun/bin/java","benchmark":"AppendBoolean","length":"50"}
   *   9   b   java.lang.String::charAt (33 bytes)
   *  10   b   sun.net.www.ParseUtil::encodePath (336 bytes)
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
   * [measurement] {"measurements":[{"nanosPerRep":663.935830809371, ... }]}
   * [caliper] [scenario finished]
   * [caliper] [scenarios finished]
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

    boolean measurementLog = line.startsWith(LogConstants.MEASUREMENT_JSON_PREFIX);
    boolean caliperLog = line.startsWith(LogConstants.CALIPER_LOG_PREFIX);
    // True if the line seems to indicate a garbage collection, like
    // [GC 1263K->363K(184576K), 0.0006660 secs]
    // or
    // [Full GC 363K->300K(184576K), 0.0053820 secs]
    boolean gcLog = line.startsWith("[GC ") || line.startsWith("[Full GC ");
    boolean compilationLog = compilationPattern.matcher(line).matches();

    if (measurementLog) {
      try {
        logEntryBuilder.setMeasurementSet(Json.measurementSetFromJson(
            line.substring(LogConstants.MEASUREMENT_JSON_PREFIX.length())));
      } catch (IllegalArgumentException ignore) {
      }
    } else if (caliperLog) {
      String caliperLogLine = line.substring(LogConstants.CALIPER_LOG_PREFIX.length());

      // timed sections start with "[caliper] [starting timed section]"
      // end with "[caliper] [done timed section]"
      if (caliperLogLine.equals(LogConstants.MEASURED_SECTION_STARTING)) {
        inTimedSection = true;
      } else if (caliperLogLine.equals(LogConstants.MEASURED_SECTION_DONE)) {
        inTimedSection = false;
      }
    }

    if (caliperLog || inTimedSection || compilationLog) {
      logEntryBuilder.setLogLine(line);
    }
    if (!caliperLog && !gcLog && !compilationLog) {
      logEntryBuilder.setDisplayLine(line);
    }

    return logEntryBuilder.build();
  }
}
