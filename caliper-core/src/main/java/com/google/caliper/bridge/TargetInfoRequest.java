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


import com.google.auto.value.AutoValue;
import com.google.common.collect.ImmutableSetMultimap;
import com.google.common.collect.Multimap;

/**
 * {@link WorkerRequest} for telling a worker to send the runner information on the target it's
 * running on, including a model of the benchmark class it produced.
 *
 * @author Colin Decker
 */
@AutoValue
public abstract class TargetInfoRequest implements WorkerRequest {
  private static final long serialVersionUID = 1L;

  public static TargetInfoRequest create(Multimap<String, String> userParameters) {
    return new AutoValue_TargetInfoRequest(ImmutableSetMultimap.copyOf(userParameters));
  }

  @Override
  public final Class<? extends WorkerRequest> type() {
    return TargetInfoRequest.class;
  }

  /**
   * Returns the parameters and their values that user provided for the benchmark, to be validated.
   */
  public abstract ImmutableSetMultimap<String, String> userParameters();
}
