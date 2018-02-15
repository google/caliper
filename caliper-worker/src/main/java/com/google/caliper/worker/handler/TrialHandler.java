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

import com.google.caliper.bridge.ShouldContinueMessage;
import com.google.caliper.bridge.StartMeasurementLogMessage;
import com.google.caliper.bridge.StopMeasurementLogMessage;
import com.google.caliper.bridge.TrialRequest;
import com.google.caliper.bridge.VmPropertiesLogMessage;
import com.google.caliper.bridge.WorkerRequest;
import com.google.caliper.model.Measurement;
import com.google.caliper.worker.connection.ClientConnectionService;
import com.google.caliper.worker.instrument.WorkerInstrument;
import com.google.caliper.worker.instrument.WorkerInstrumentFactory;
import java.io.IOException;
import javax.inject.Inject;

/**
 * Handler for a {@link TrialRequest}.
 *
 * @author Colin Decker
 */
final class TrialHandler implements RequestHandler {

  private final ClientConnectionService clientConnection;
  private final WorkerInstrumentFactory instrumentFactory;

  @Inject
  TrialHandler(
      ClientConnectionService clientConnection, WorkerInstrumentFactory instrumentFactory) {
    this.clientConnection = clientConnection;
    this.instrumentFactory = instrumentFactory;
  }

  @Override
  public void handleRequest(WorkerRequest request) throws Exception {
    TrialRequest trialRequest = (TrialRequest) request;
    WorkerInstrument workerInstrument =
        instrumentFactory.createWorkerInstrument(trialRequest.experiment());

    notifyVmProperties();
    try {
      workerInstrument.setUpBenchmark();
      notifyTrialBootstrapPhaseStarting();
      workerInstrument.bootstrap();
      notifyTrialMeasurementPhaseStarting();
      boolean keepMeasuring = true;
      boolean isInWarmup = true;
      while (keepMeasuring) {
        workerInstrument.preMeasure(isInWarmup);
        notifyTrialMeasurementStarting();
        try {
          ShouldContinueMessage message = notifyTrialMeasurementEnding(workerInstrument.measure());
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

  private void notifyVmProperties() throws IOException {
    clientConnection.send(new VmPropertiesLogMessage());
  }

  private void notifyTrialBootstrapPhaseStarting() throws IOException {
    clientConnection.send("Bootstrap phase starting.");
  }

  private void notifyTrialMeasurementPhaseStarting() throws IOException {
    clientConnection.send("Measurement phase starting (includes warmup and actual measurement).");
  }

  private void notifyTrialMeasurementStarting() throws IOException {
    clientConnection.send("About to measure.", new StartMeasurementLogMessage());
  }

  /**
   * Report the measurements and wait for it to be ack'd by the runner. Returns a message received
   * from the runner, which lets us know whether to continue measuring and whether we're in the
   * warmup or measurement phase.
   */
  private ShouldContinueMessage notifyTrialMeasurementEnding(Iterable<Measurement> measurements)
      throws IOException {
    clientConnection.send(new StopMeasurementLogMessage(measurements));
    return (ShouldContinueMessage) clientConnection.receive();
  }
}
