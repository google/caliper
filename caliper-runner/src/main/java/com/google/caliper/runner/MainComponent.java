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

import com.google.caliper.runner.config.CaliperConfig;
import com.google.caliper.runner.options.CaliperOptions;
import com.google.common.collect.ImmutableSet;
import com.google.common.util.concurrent.ServiceManager;

/**
 * The main component used when running Caliper.
 *
 * <p>This class provides the methods for the component, but is not actually annotated with
 * {@code @Component}. Annotated subclasses exist for each supported platform to allow a different
 * set of module dependencies for each.
 */
interface MainComponent {

  BenchmarkClass getBenchmarkClass();

  CaliperConfig getCaliperConfig();

  CaliperOptions getCaliperOptions();

  CaliperRun getCaliperRun();

  ServiceManager getServiceManager();

  ImmutableSet<Instrument> instruments();

  TrialScopeComponent newTrialComponent(TrialModule trialModule);

  ExperimentComponent newExperimentComponent(ExperimentModule experimentModule);
}
