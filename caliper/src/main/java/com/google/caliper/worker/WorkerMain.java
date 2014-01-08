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

import static com.google.inject.Stage.PRODUCTION;

import com.google.caliper.bridge.BridgeModule;
import com.google.caliper.bridge.WorkerSpec;
import com.google.caliper.json.GsonModule;
import com.google.caliper.runner.BenchmarkClassModule;
import com.google.caliper.runner.ExperimentModule;
import com.google.gson.Gson;
import com.google.inject.Guice;
import com.google.inject.Injector;

/**
 * This class is invoked as a subprocess by the Caliper runner parent process; it re-stages
 * the benchmark and hands it off to the instrument's worker.
 */
public final class WorkerMain {
  private WorkerMain() {}

  public static void main(String[] args) throws Exception {
    Injector gsonInjector = Guice.createInjector(PRODUCTION, new GsonModule());
    WorkerSpec request =
        gsonInjector.getInstance(Gson.class).fromJson(args[0], WorkerSpec.class);

    Injector workerInjector = gsonInjector.createChildInjector(
        new BenchmarkClassModule(Class.forName(request.benchmarkSpec.className())),
        ExperimentModule.forWorkerSpec(request),
        new BridgeModule(),
        new WorkerModule(request));

    Worker worker = workerInjector.getInstance(Worker.class);
    WorkerEventLog log = workerInjector.getInstance(WorkerEventLog.class);

    log.notifyWorkerStarted(request.trialId);
    try {
      worker.setUpBenchmark();
      log.notifyWarmupPhaseStarting();
      worker.bootstrap();
      log.notifyMeasurementPhaseStarting();
      boolean keepMeasuring = true;
      while (keepMeasuring) {
        worker.preMeasure();
        log.notifyMeasurementStarting();
        try {
          keepMeasuring = log.notifyMeasurementEnding(worker.measure());
        } finally {
          worker.postMeasure();
        }
      }
    } catch (Exception e) {
      log.notifyFailure(e);
    } finally {
      System.out.flush(); // ?
      worker.tearDownBenchmark();
    }
  }
}
