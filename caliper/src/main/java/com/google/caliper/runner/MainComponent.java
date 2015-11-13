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
import com.google.caliper.options.CaliperOptions;
import com.google.caliper.options.OptionsModule;
import com.google.caliper.util.OutputModule;
import com.google.common.util.concurrent.ServiceManager;

import dagger.Component;

import javax.inject.Singleton;

/**
 * The main component used when running caliper.
 */
@Singleton
@Component(modules = {
    BenchmarkClassModule.class,
    BridgeModule.class,
    ConfigModule.class,
    ExperimentingRunnerModule.class,
    GsonModule.class,
    MainModule.class,
    OptionsModule.class,
    OutputModule.class,
    PlatformModule.class,
    RunnerModule.class,
    ServiceModule.class,
})
interface MainComponent {

  BenchmarkClass getBenchmarkClass();

  CaliperConfig getCaliperConfig();

  CaliperOptions getCaliperOptions();

  CaliperRun getCaliperRun();

  ServiceManager getServiceManager();

  TrialScopeComponent newTrialComponent(TrialModule trialModule);

  ExperimentComponent newExperimentComponent(ExperimentModule experimentModule);
}
