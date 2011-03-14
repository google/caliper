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

package com.google.caliper.spi;

import com.google.caliper.runner.BenchmarkClass;
import com.google.caliper.runner.BenchmarkMethod;
import com.google.caliper.runner.CaliperOptions;
import com.google.caliper.runner.MicrobenchmarkInstrument;
import com.google.caliper.runner.Scenario;
import com.google.caliper.runner.ScenarioSet;
import com.google.common.collect.ImmutableMap;

import java.lang.reflect.Method;

public abstract class Instrument {
  public int estimateRuntimeSeconds(ScenarioSet scenarios, CaliperOptions options) {
    throw new UnsupportedOperationException();
  }

  public abstract boolean isBenchmarkMethod(Method method);

  public abstract BenchmarkMethod createBenchmarkMethod(
      BenchmarkClass benchmarkClass, Method method);

  public abstract void dryRun(Scenario scenario);
}
