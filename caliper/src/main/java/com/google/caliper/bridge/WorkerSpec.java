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
import com.google.common.collect.ImmutableMap;

/**
 * This object is sent from the parent process to the child to tell it what to do. If the child
 * does not do it, it will not get its allowance this week.
 */
public final class WorkerSpec {
  public final String workerClassName;
  public final ImmutableMap<String, String> workerOptions;
  public final BenchmarkSpec benchmarkSpec;
  public final String pipePath;

  public WorkerSpec(
      String workerClassName,
      ImmutableMap<String, String> workerOptions,
      BenchmarkSpec benchmarkSpec,
      String pipePath) {
    this.workerClassName = workerClassName;
    this.workerOptions = workerOptions;
    this.benchmarkSpec = benchmarkSpec;
    this.pipePath = pipePath;
  }
}
