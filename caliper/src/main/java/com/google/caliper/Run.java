/**
 * Copyright (C) 2009 Google Inc.
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

import com.google.common.annotations.GwtCompatible;

import java.io.Serializable;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * The complete result of a benchmark suite run.
 *
 * WARNING: a JSON representation of this class is stored on the app engine server. If any changes
 * are made to this class, a deserialization adapter must be written for this class to ensure
 * backwards compatibility.
 *
 * <p>Gwt-safe.
 */
@SuppressWarnings("serial")
@GwtCompatible
public final class Run
    implements Serializable /* for GWT Serialization */ {

  private /*final*/ Map<Scenario, ScenarioResult> measurements;
  private /*final*/ String benchmarkName;
  private /*final*/ long executedTimestamp;

  // TODO: add more run properties such as checksums of the executed code

  public Run(Map<Scenario, ScenarioResult> measurements,
      String benchmarkName, Date executedTimestamp) {
    if (benchmarkName == null || executedTimestamp == null) {
      throw new NullPointerException();
    }

    this.measurements = new LinkedHashMap<Scenario, ScenarioResult>(measurements);
    this.benchmarkName = benchmarkName;
    this.executedTimestamp = executedTimestamp.getTime();
  }

  public Map<Scenario, ScenarioResult> getMeasurements() {
    return measurements;
  }

  public String getBenchmarkName() {
    return benchmarkName;
  }

  public Date getExecutedTimestamp() {
    return new Date(executedTimestamp);
  }

  @Override public boolean equals(Object o) {
    if (o instanceof Run) {
      Run that = (Run) o;
      return measurements.equals(that.measurements)
          && benchmarkName.equals(that.benchmarkName)
          && executedTimestamp == that.executedTimestamp;
    }

    return false;
  }

  @Override public int hashCode() {
    int result = measurements.hashCode();
    result = result * 37 + benchmarkName.hashCode();
    result = result * 37 + (int) ((executedTimestamp >> 32) ^ executedTimestamp);
    return result;
  }

  @Override public String toString() {
    return measurements.toString();
  }

  @SuppressWarnings("unused")
  private Run() {} // for GWT Serialization
}
