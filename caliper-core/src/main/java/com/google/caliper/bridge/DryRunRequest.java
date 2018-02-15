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

import com.google.common.collect.ImmutableSet;

/**
 * {@link WorkerRequest} for telling the worker to do a dry-run of multiple experiments.
 *
 * @author Colin Decker
 */
public final class DryRunRequest implements WorkerRequest {
  private static final long serialVersionUID = 1L;

  private final ImmutableSet<ExperimentSpec> experiments;

  public DryRunRequest(Iterable<ExperimentSpec> experiments) {
    this.experiments = ImmutableSet.copyOf(experiments);
  }

  @Override
  public final Class<? extends WorkerRequest> type() {
    return DryRunRequest.class;
  }

  /** Returns the set of experiments to be dry-run. */
  public ImmutableSet<ExperimentSpec> experiments() {
    return experiments;
  }
}
