/*
 * Copyright (C) 2017 Google Inc.
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

import static com.google.common.base.Preconditions.checkArgument;

/**
 * {@link WorkerRequest} for telling a worker to send the runner the information it needs on the
 * benchmark, such as what methods and parameters it has.
 *
 * @author Colin Decker
 */
public final class BenchmarkInfoRequest implements WorkerRequest {
  private static final long serialVersionUID = 1L;

  private final String benchmarkClass;

  public BenchmarkInfoRequest(String benchmarkClass) {
    checkArgument(!benchmarkClass.isEmpty());
    this.benchmarkClass = benchmarkClass;
  }

  /**
   * Returns the name of the benchmark class to get info for.
   */
  public String benchmarkClass() {
    return benchmarkClass;
  }
}
