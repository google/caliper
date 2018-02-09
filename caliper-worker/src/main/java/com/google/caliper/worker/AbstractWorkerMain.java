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
import com.google.caliper.bridge.BenchmarkModelRequest;
import com.google.caliper.bridge.DryRunRequest;
import com.google.caliper.bridge.ExperimentSpec;
import com.google.caliper.bridge.OpenedSocket;
import com.google.caliper.bridge.ShouldContinueMessage;
import com.google.caliper.bridge.TrialRequest;
import com.google.caliper.bridge.WorkerRequest;
import com.google.caliper.core.UserCodeException;
import com.google.caliper.model.BenchmarkClassModel;
import com.google.common.collect.ImmutableSet;
import com.google.common.io.Closer;
import java.io.IOException;
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

      try {
        if (request instanceof BenchmarkModelRequest) {
          log.sendBenchmarkModel(getBenchmarkModel((BenchmarkModelRequest) request));
        } else if (request instanceof DryRunRequest) {
          log.notifyDryRunSuccess(dryRun(((DryRunRequest) request).experiments()));
        } else {
          runTrial(((TrialRequest) request).experiment(), log);
        }
      } catch (IOException e) {
        // If an IOException was thrown, it was probably from trying to send something to the
        // runner and failing, so don't bother trying to send *that* to the runner.
        throw e;
      } catch (Exception e) {
        log.notifyFailure(e);
      }
    } catch (Throwable t) {
      throw closer.rethrow(t, Exception.class);
    } finally {
      closer.close();
    }
  }

  private BenchmarkClassModel getBenchmarkModel(BenchmarkModelRequest request) throws Exception {
    BenchmarkClass benchmarkClass = BenchmarkClass.forName(request.benchmarkClass());
    benchmarkClass.validateParameters(request.userParameters());
    return benchmarkClass.toModel();
  }

  private ImmutableSet<Integer> dryRun(Iterable<ExperimentSpec> experiments) throws Exception {
    ImmutableSet.Builder<Integer> successes = ImmutableSet.builder();

    for (ExperimentSpec experiment : experiments) {
      try {
        // Worker creation done here to ensure that user code exceptions thrown in construction are
        // handled the same as exceptions thrown from benchmark methods, setup methods, etc.
        WorkerInstrument workerInstrument = createWorkerInstrument(experiment);

        workerInstrument.setUpBenchmark();
        try {
          workerInstrument.dryRun();
        } finally {
          workerInstrument.tearDownBenchmark();
        }

        successes.add(experiment.id());
      } catch (InvocationTargetException e) {
        Throwable userException = e.getCause(); // the exception thrown by the benchmark method
        if (userException instanceof SkipThisScenarioException) {
          // Throwing SkipThisScenarioException is not a failure; we simply don't include that
          // experiment's ID in the list we send back, which tells the runner that it should be
          continue;
          // skipped.
        }

        throw new UserCodeException(userException);
      }
    }

    return successes.build();
  }

  private void runTrial(ExperimentSpec experiment, WorkerEventLog log) throws Exception {
    WorkerInstrument workerInstrument = createWorkerInstrument(experiment);

    log.notifyVmProperties();
    try {
      workerInstrument.setUpBenchmark();
      log.notifyTrialBootstrapPhaseStarting();
      workerInstrument.bootstrap();
      log.notifyTrialMeasurementPhaseStarting();
      boolean keepMeasuring = true;
      boolean isInWarmup = true;
      while (keepMeasuring) {
        workerInstrument.preMeasure(isInWarmup);
        log.notifyTrialMeasurementStarting();
        try {
          ShouldContinueMessage message =
              log.notifyTrialMeasurementEnding(workerInstrument.measure());
          keepMeasuring = message.shouldContinue();
          isInWarmup = !message.isWarmupComplete();
        } finally {
          workerInstrument.postMeasure();
        }
      }
    } finally {
      workerInstrument.tearDownBenchmark();
    }
  }

  private WorkerInstrument createWorkerInstrument(ExperimentSpec experiment) {
    return createWorkerComponent(experiment).getWorkerInstrument();
  }

  /**
   * Creates the Dagger {@link WorkerComponent} that will create the worker for the given
   * experiment.
   */
  protected abstract WorkerComponent createWorkerComponent(ExperimentSpec experiment);
}
