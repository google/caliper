/*
 * Copyright (C) 2018 Google Inc.
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

package com.google.caliper.bridge;

import com.google.caliper.model.BenchmarkSpec;
import com.google.caliper.model.InstrumentType;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import java.io.Serializable;

/**
 * Specification of an experiment to be run as a dry-run or a trial on a worker.
 *
 * @author Colin Decker
 */
public final class ExperimentSpec implements Serializable {
  private static final long serialVersionUID = 1L;

  private final int id;
  private final InstrumentType instrumentType;
  private final ImmutableMap<String, String> workerInstrumentOptions;
  private final BenchmarkSpec benchmarkSpec;
  private final ImmutableList<String> methodParameterClasses;

  public ExperimentSpec(
      int id,
      InstrumentType instrumentType,
      ImmutableMap<String, String> workerInstrumentOptions,
      BenchmarkSpec benchmarkSpec,
      Iterable<String> methodParameterClasses) {
    this.id = id;
    this.instrumentType = instrumentType;
    this.workerInstrumentOptions = workerInstrumentOptions;
    this.benchmarkSpec = benchmarkSpec;
    this.methodParameterClasses = ImmutableList.copyOf(methodParameterClasses);
  }

  /** Returns the ID of this experiment. */
  public int id() {
    return id;
  }

  /** Returns the type of instrument to use for the experiment. */
  public InstrumentType instrumentType() {
    return instrumentType;
  }

  /** Returns the worker options to use. */
  public ImmutableMap<String, String> workerInstrumentOptions() {
    return workerInstrumentOptions;
  }

  /** Returns the spec of the benchmark to run. */
  public BenchmarkSpec benchmarkSpec() {
    return benchmarkSpec;
  }

  /**
   * Returns the parameter types for the benchmark method to run so that it can be uniquely
   * identified.
   */
  public ImmutableList<String> methodParameterClasses() {
    return methodParameterClasses;
  }
}
