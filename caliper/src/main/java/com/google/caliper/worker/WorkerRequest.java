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

import com.google.caliper.util.Util;

import java.util.Map;

/**
 * This object is sent from the parent process to the child to tell it what to do. If the child
 * does not do it, it will not get its allowance this week.
 */
public final class WorkerRequest {
  public static WorkerRequest fromString(String json) {
    return Util.GSON.fromJson(json, WorkerRequest.class);
  }

  public final Map<String, String> instrumentOptions;
  public final String workerClassName;
  public final String benchmarkClassName;

  // TODO(kevinb): in the future we may have a list of scenarios
  public final String benchmarkMethodName;
  public final Map<String, String> injectedParameters;
  public final Map<String, String> vmArguments;

  public WorkerRequest(
      Map<String, String> instrumentOptions,
      String workerClassName,
      String benchmarkClassName,
      String benchmarkMethodName,
      Map<String, String> injectedParameters,
      Map<String, String> vmArguments) {
    this.instrumentOptions = instrumentOptions;
    this.workerClassName = workerClassName;
    this.benchmarkClassName = benchmarkClassName;
    this.benchmarkMethodName = benchmarkMethodName;
    this.injectedParameters = injectedParameters;
    this.vmArguments = vmArguments;
  }

  @Override public String toString() {
    return Util.GSON.toJson(this);
  }
}
