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
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;

import java.io.Serializable;
import java.util.UUID;

/**
 * This object is sent from the parent process to the child to tell it what to do. If the child
 * does not do it, it will not get its allowance this week.
 */
public final class WorkerSpec implements Serializable {
  private static final long serialVersionUID = 1L;

  public final UUID trialId;
  // Should be ? extends Worker but circular build deps prevent that.
  public final Class<?> workerClass;
  public final ImmutableMap<String, String> workerOptions;
  public final BenchmarkSpec benchmarkSpec;

  /**
   * The names of the benchmark method parameters so that the method can be uniquely identified.
   */
  public final ImmutableList<Class<?>> methodParameterClasses;
  public final int port;

  public WorkerSpec(
      UUID trialId,
      Class<?> workerClass,
      ImmutableMap<String, String> workerOptions,
      BenchmarkSpec benchmarkSpec,
      ImmutableList<Class<?>> methodParameterClasses,
      int port) {
    this.trialId = trialId;
    this.workerClass = workerClass;
    this.workerOptions = workerOptions;
    this.benchmarkSpec = benchmarkSpec;
    this.methodParameterClasses = methodParameterClasses;
    this.port = port;
  }
}
