/*
 * Copyright (C) 2011 Google Inc.
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

package com.google.caliper.worker;

import com.google.caliper.model.ArbitraryMeasurement;
import com.google.caliper.model.Measurement;
import com.google.caliper.model.Value;
import com.google.caliper.util.Util;

import java.lang.reflect.Method;
import java.util.Map;

/**
 * Worker for arbitrary measurements.
 */
public final class ArbitraryMeasurementWorker implements Worker {
  @Override
  public void measure(Object benchmark, String methodName,
      Map<String, String> optionsMap, WorkerEventLog log) throws Exception {

    Options options = new Options(optionsMap);
    Method method = benchmark.getClass().getDeclaredMethod(methodName);
    ArbitraryMeasurement annotation = method.getAnnotation(ArbitraryMeasurement.class);
    String unit = annotation.units();
    String description = annotation.description();

    // measure
    log.notifyMeasurementPhaseStarting();

    if (options.gcBeforeEach) {
      Util.forceGc();
    }

    log.notifyMeasurementStarting();
    double measured = (Double) method.invoke(benchmark);
    log.notifyMeasurementEnding(new Measurement.Builder()
        .value(Value.create(measured, unit))
        .weight(1)
        .description(description)
        .build());
  }

  private static class Options {
    final boolean gcBeforeEach;

    Options(Map<String, String> options) {
      this.gcBeforeEach = Boolean.parseBoolean(options.get("gcBeforeEach"));
    }
  }
}
