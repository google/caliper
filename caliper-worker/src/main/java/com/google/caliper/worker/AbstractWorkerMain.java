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

import com.google.caliper.api.SkipThisScenarioException;
import com.google.caliper.bridge.DryRunRequest;
import com.google.caliper.bridge.ExperimentSpec;
import com.google.caliper.bridge.OpenedSocket;
import com.google.caliper.bridge.ShouldContinueMessage;
import com.google.caliper.bridge.TrialRequest;
import com.google.caliper.bridge.WorkerRequest;
import com.google.caliper.core.UserCodeException;
import com.google.common.collect.ImmutableSet;
import com.google.common.io.Closer;
import java.lang.reflect.InvocationTargetException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.channels.SocketChannel;
import java.util.UUID;

/**
 * This class is invoked as a subprocess by the Caliper runner parent process; it re-stages the
 * benchmark and hands it off to the instrument's worker.
 */
abstract class AbstractWorkerMain {
  protected final void mainImpl(String[] args) throws Exception {
    UUID id = UUID.fromString(args[0]);
    int port = Integer.valueOf(args[1]);

    Closer closer = Closer.create();
    try {
      SocketChannel channel = closer.register(SocketChannel.open());
      channel.connect(new InetSocketAddress(InetAddress.getLoopbackAddress(), port));

      WorkerEventLog log = closer.register(new WorkerEventLog(OpenedSocket.fromSocket(channel)));

      WorkerRequest request = log.notifyWorkerStarted(id);

      if (request instanceof DryRunRequest) {
        dryRun(((DryRunRequest) request).experiments(), log);
      } else {
        runTrial(((TrialRequest) request).experiment(), log);
      }
    } catch (Throwable t) {
      throw closer.rethrow(t, Exception.class);
    } finally {
      closer.close();
    }
  }

  private void dryRun(Iterable<ExperimentSpec> experiments, WorkerEventLog log) throws Exception {
    ImmutableSet.Builder<Integer> successes = ImmutableSet.builder();

    for (ExperimentSpec experiment : experiments) {
      try {
        // Worker creation done here to ensure that user code exceptions thrown in construction are
        // handled the same as exceptions thrown from benchmark methods, setup methods, etc.
        Worker worker = createWorker(experiment);

        worker.setUpBenchmark();
        try {
          worker.dryRun();
        } finally {
          worker.tearDownBenchmark();
        }

        successes.add(experiment.id());
      } catch (Throwable e) {
        if (e instanceof InvocationTargetException) {
          Throwable userException = e.getCause(); // the exception thrown by the benchmark method
          if (userException instanceof SkipThisScenarioException) {
            // Throwing SkipThisScenarioException is not a failure; we simply don't include that
            // experiment's ID in the list we send back, which tells the runner that it should be
            // skipped.
            continue;
          }

          e = new UserCodeException(userException);
        }

        log.notifyFailure(e);
        // stop after one failure; the runner should throw an exception once we notify it anyway
        return;
      }
    }

    log.notifyDryRunSuccess(successes.build());
  }

  private void runTrial(ExperimentSpec experiment, WorkerEventLog log) throws Exception {
    Worker worker = createWorker(experiment);

    log.notifyVmProperties();
    try {
      worker.setUpBenchmark();
      log.notifyTrialBootstrapPhaseStarting();
      worker.bootstrap();
      log.notifyTrialMeasurementPhaseStarting();
      boolean keepMeasuring = true;
      boolean isInWarmup = true;
      while (keepMeasuring) {
        worker.preMeasure(isInWarmup);
        log.notifyTrialMeasurementStarting();
        try {
          ShouldContinueMessage message = log.notifyTrialMeasurementEnding(worker.measure());
          keepMeasuring = message.shouldContinue();
          isInWarmup = !message.isWarmupComplete();
        } finally {
          worker.postMeasure();
        }
      }
    } catch (Exception e) {
      log.notifyFailure(e);
    } finally {
      worker.tearDownBenchmark();
    }
  }

  private Worker createWorker(ExperimentSpec experiment) {
    return createWorkerComponent(experiment).getWorker();
  }

  /**
   * Creates the Dagger {@link WorkerComponent} that will create the worker for the given
   * experiment.
   */
  protected abstract WorkerComponent createWorkerComponent(ExperimentSpec experiment);
}
