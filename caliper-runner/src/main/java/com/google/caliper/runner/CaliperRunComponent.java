/*
 * Copyright (C) 2015 Google Inc.
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

package com.google.caliper.runner;

import dagger.Subcomponent;

/** The component for a {@link CaliperRun}. */
@RunScoped
@Subcomponent(
  modules = {
    BenchmarkClassModule.class,
    CaliperRunModule.class,
  }
)
interface CaliperRunComponent {

  /** Returns the Caliper benchmark run. */
  CaliperRun getCaliperRun();

  /** Creates a new component containing the runner classes needed for running a single trial. */
  TrialScopeComponent newTrialComponent(TrialModule trialModule);

  /** Returns a new component for running an experiment. */
  // This is currently only used for performing a dry-run.
  // TODO(cgdecker): Remove this when moving dry runs to the worker
  ExperimentComponent newExperimentComponent(ExperimentModule experimentModule);
}
