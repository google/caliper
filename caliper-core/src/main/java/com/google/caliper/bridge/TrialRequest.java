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

package com.google.caliper.bridge;

import com.google.caliper.model.BenchmarkSpec;
import com.google.caliper.model.InstrumentType;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import java.util.UUID;

/**
 * {@link WorkerRequest} for telling the worker to run a trial of the benchmark.
 *
 * @author Colin Decker
 */
public final class TrialRequest extends WorkerRequest {
  private static final long serialVersionUID = 1L;

  private final UUID trialId;
  private final InstrumentType instrumentType;
  private final ImmutableMap<String, String> workerOptions;
  private final BenchmarkSpec benchmarkSpec;
  private final ImmutableList<Class<?>> methodParameterClasses;

  public TrialRequest(
      UUID trialId,
      InstrumentType instrumentType,
      ImmutableMap<String, String> workerOptions,
      BenchmarkSpec benchmarkSpec,
      ImmutableList<Class<?>> methodParameterClasses,
      int port) {
    super(port);
    this.trialId = trialId;
    this.instrumentType = instrumentType;
    this.workerOptions = workerOptions;
    this.benchmarkSpec = benchmarkSpec;
    this.methodParameterClasses = methodParameterClasses;
  }

  /**
   * Returns the ID of the trial to run.
   */
  public UUID trialId() {
    return trialId;
  }

  /**
   * Returns the instrument to use for the trial.
   */
  public InstrumentType instrumentType() {
    return instrumentType;
  }

  /**
   * Returns the worker options to use.
   */
  public ImmutableMap<String, String> workerOptions() {
    return workerOptions;
  }

  /**
   * Returns the spec of the benchmark to run for the trial.
   */
  public BenchmarkSpec benchmarkSpec() {
    return benchmarkSpec;
  }

  /**
   * Returns the parameter types for the benchmark method to run so that it can be uniquely
   * identified.
   */
  public ImmutableList<Class<?>> methodParameterClasses() {
    return methodParameterClasses;
  }
}
