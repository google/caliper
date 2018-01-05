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

import java.util.UUID;

/**
 * {@link WorkerRequest} for telling the worker to run a trial of the benchmark.
 *
 * @author Colin Decker
 */
public final class TrialRequest extends WorkerRequest {
  private static final long serialVersionUID = 1L;

  private final ExperimentSpec experiment;

  public TrialRequest(UUID id, int port, ExperimentSpec experiment) {
    super(id, port);
    this.experiment = experiment;
  }

  /** Returns the experiment to run. */
  public ExperimentSpec experiment() {
    return experiment;
  }
}
