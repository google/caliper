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

import com.google.caliper.bridge.ExperimentSpec;

/**
 * This class is invoked as a subprocess by the Caliper runner parent process; it re-stages the
 * benchmark and hands it off to the instrument's worker.
 */
public final class WorkerMain extends AbstractWorkerMain {
  private WorkerMain() {}

  public static void main(String[] args) throws Exception {
    new WorkerMain().mainImpl(args);
  }

  @Override
  protected WorkerComponent createWorkerComponent(ExperimentSpec experiment) {
    return DaggerJvmWorkerComponent.builder().experiment(experiment).build();
  }
}
