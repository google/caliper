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

/**
 * Represents a single, complete chunk of information from the log emitted from the
 * InProcessRunner. It does not necessarily correspond one-to-one with a line from this log.
 */
public final class LogEntry {

  private final MeasurementSet measurementSet;
  private final String logLine;
  private final String displayLine;

  public LogEntry(MeasurementSet measurementSet, String logLine, String displayLine) {
    this.measurementSet = measurementSet;
    this.logLine = logLine;
    this.displayLine = displayLine;
  }

  public boolean hasMeasurementSet() {
    return measurementSet != null;
  }

  public MeasurementSet getMeasurementSet() {
    return measurementSet;
  }

  public boolean log() {
    return logLine != null;
  }

  public String logLine() {
    return logLine;
  }

  public boolean display() {
    return displayLine != null;
  }

  public String displayLine() {
    return displayLine;
  }

  public static class LogEntryBuilder {
    private MeasurementSet builderMeasurementSet;
    private String builderLogLine;
    private String builderDisplayLine;

    public LogEntryBuilder setMeasurementSet(MeasurementSet measurementSet) {
      this.builderMeasurementSet = measurementSet;
      return this;
    }

    public LogEntryBuilder setLogLine(String logLine) {
      this.builderLogLine = logLine;
      return this;
    }

    public LogEntryBuilder setDisplayLine(String displayLine) {
      this.builderDisplayLine = displayLine;
      return this;
    }

    public LogEntry build() {
      return new LogEntry(builderMeasurementSet, builderLogLine, builderDisplayLine);
    }
  }
}
