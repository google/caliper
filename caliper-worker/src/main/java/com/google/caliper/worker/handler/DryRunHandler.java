/*
 * Copyright (C) 2018 Google Inc.
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

package com.google.caliper.worker.handler;

import com.google.caliper.api.SkipThisScenarioException;
import com.google.caliper.bridge.DryRunRequest;
import com.google.caliper.bridge.DryRunSuccessLogMessage;
import com.google.caliper.bridge.ExperimentSpec;
import com.google.caliper.bridge.WorkerRequest;
import com.google.caliper.core.UserCodeException;
import com.google.caliper.worker.connection.ClientConnectionService;
import com.google.caliper.worker.instrument.WorkerInstrument;
import com.google.caliper.worker.instrument.WorkerInstrumentFactory;
import com.google.common.collect.ImmutableSet;
import java.lang.reflect.InvocationTargetException;
import javax.inject.Inject;

/** Handler for a {@link DryRunRequest}. */
final class DryRunHandler implements RequestHandler {

  private final ClientConnectionService clientConnection;
  private final WorkerInstrumentFactory instrumentFactory;

  @Inject
  DryRunHandler(
      ClientConnectionService clientConnection, WorkerInstrumentFactory instrumentFactory) {
    this.clientConnection = clientConnection;
    this.instrumentFactory = instrumentFactory;
  }

  @Override
  public void handleRequest(WorkerRequest request) throws Exception {
    DryRunRequest dryRunRequest = (DryRunRequest) request;

    ImmutableSet.Builder<Integer> successes = ImmutableSet.builder();
    for (ExperimentSpec experiment : dryRunRequest.experiments()) {
      try {
        WorkerInstrument workerInstrument = instrumentFactory.createWorkerInstrument(experiment);

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

    clientConnection.send(DryRunSuccessLogMessage.create(successes.build()));
  }
}
