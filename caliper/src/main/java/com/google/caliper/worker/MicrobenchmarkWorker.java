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

import com.google.caliper.Benchmark;
import com.google.caliper.util.SimpleDuration;

import java.lang.reflect.Method;
import java.util.Map;

public class MicrobenchmarkWorker implements Worker {
  @Override public void runTrial(
      Benchmark benchmark, String methodName, Map<String, String> options) throws Exception {
    SimpleDuration minWarmup = SimpleDuration.valueOf(options.get("minWarmup"));
    SimpleDuration timingInterval = SimpleDuration.valueOf(options.get("timingInterval"));
    int reportedIntervals = Integer.parseInt(options.get("reportedIntervals"));
    double shortCircuitTolerance = Double.parseDouble(options.get("shortCircuitTolerance"));
    SimpleDuration maxTotalRuntime = SimpleDuration.valueOf(options.get("maxTotalRuntime"));
    boolean gcBeforeEach = Boolean.parseBoolean(options.get("gcBeforeEach"));

    invoke(benchmark, "time" + methodName, 1);
  }

  private static void invoke(Object object, String methodName, int i) throws Exception {
    Method method = object.getClass().getMethod(methodName, int.class);
    method.setAccessible(true);
    method.invoke(object, i);
  }
}
