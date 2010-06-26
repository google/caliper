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

import com.google.common.base.Objects;
import java.io.Serializable;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * The complete result of a benchmark suite run.
 *
 * <p>Gwt-safe.
 */
@SuppressWarnings("serial")
public final class Run
    implements Serializable /* for GWT Serialization */ {

  private /*final*/ Map<Scenario, MeasurementSet> measurements;
  private /*final*/ String benchmarkName;
  private /*final*/ String apiKey;
  private /*final*/ long executedTimestamp;

  // TODO: add more run properites such as checksums of the executed code

  public Run(Map<Scenario, MeasurementSet> measurements,
      String benchmarkName, String apiKey, Date executedTimestamp) {
    if (benchmarkName == null || executedTimestamp == null) {
      throw new NullPointerException();
    }

    this.measurements = new LinkedHashMap<Scenario, MeasurementSet>(measurements);
    this.benchmarkName = benchmarkName;
    this.apiKey = apiKey;
    this.executedTimestamp = executedTimestamp.getTime();
  }

  public Map<Scenario, MeasurementSet> getMeasurements() {
    return measurements;
  }

  public String getBenchmarkName() {
    return benchmarkName;
  }

  public String getApiKey() {
    return apiKey;
  }

  public Date getExecutedTimestamp() {
    return new Date(executedTimestamp);
  }

  @Override public boolean equals(Object o) {
    if (o instanceof Run) {
      Run that = (Run) o;
      return measurements.equals(that.measurements)
          && benchmarkName.equals(that.benchmarkName)
          && Objects.equal(apiKey, that.apiKey)
          && executedTimestamp == that.executedTimestamp;
    }

    return false;
  }

  @Override public int hashCode() {
    return Objects.hashCode(measurements, benchmarkName, apiKey, executedTimestamp);
  }

  @Override public String toString() {
    return measurements.toString();
  }

  private Run() {} // for GWT Serialization
}
