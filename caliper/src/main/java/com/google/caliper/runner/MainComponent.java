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

import com.google.caliper.bridge.BridgeModule;
import com.google.caliper.config.CaliperConfig;
import com.google.caliper.config.ConfigModule;
import com.google.caliper.json.GsonModule;
import com.google.caliper.options.CaliperOptionsComponent;
import com.google.caliper.util.MainScope;
import com.google.caliper.util.OutputModule;
import com.google.common.util.concurrent.ServiceManager;
import dagger.Component;

/**
 * The main component used when running caliper.
 */
@MainScope
@Component(modules = {
    BenchmarkClassModule.class,
    BridgeModule.class,
    ConfigModule.class,
    ExperimentingRunnerModule.class,
    GsonModule.class,
    RunnerModule.class,
    OutputModule.class,
    ServiceModule.class,
}, dependencies = {
    CaliperOptionsComponent.class
})
interface MainComponent {

  BenchmarkClass getBenchmarkClass();

  CaliperConfig getCaliperConfig();

  CaliperRun getCaliperRun();

  ServiceManager getServiceManager();

  TrialScopeComponent newTrialComponent(TrialModule trialModule);

  ExperimentComponent newExperimentComponent(ExperimentModule experimentModule);

  InstrumentComponent newInstrumentComponent(InstrumentInjectorModule instrumentInjectorModule);
}
