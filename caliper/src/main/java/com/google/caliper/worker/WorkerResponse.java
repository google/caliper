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

import com.google.gson.Gson;

import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * This object is sent from the parent process to the child to tell it what to do. If the child
 * does not do it, it will not get its allowance this week.
 */
public class WorkerResponse {
  public static WorkerResponse fromString(String json) {
    return new Gson().fromJson(json, WorkerResponse.class);
  }

  public final Collection<Measurement> measurements;

  public WorkerResponse(Collection<Measurement> measurements) {
    this.measurements = measurements;
  }

  private WorkerResponse() {
    measurements = null;
  }

  @Override public String toString() {
    return new Gson().toJson(this);
  }
}
