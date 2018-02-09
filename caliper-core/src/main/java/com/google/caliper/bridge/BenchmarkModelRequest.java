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

import com.google.auto.value.AutoValue;
import com.google.common.collect.ImmutableSetMultimap;
import com.google.common.collect.Multimap;

/**
 * {@link WorkerRequest} for telling a worker to send the runner a model of the benchmark class.
 *
 * @author Colin Decker
 */
@AutoValue
public abstract class BenchmarkModelRequest implements WorkerRequest {
  private static final long serialVersionUID = 1L;

  public static BenchmarkModelRequest create(
      String benchmarkClass, Multimap<String, String> userParameters) {
    checkArgument(!benchmarkClass.isEmpty());
    return new AutoValue_BenchmarkModelRequest(
        benchmarkClass, ImmutableSetMultimap.copyOf(userParameters));
  }

  /** Returns the name of the benchmark class to get the model of. */
  public abstract String benchmarkClass();

  /**
   * Returns the parameters and their values that user provided for the benchmark, to be validated.
   */
  public abstract ImmutableSetMultimap<String, String> userParameters();
}
