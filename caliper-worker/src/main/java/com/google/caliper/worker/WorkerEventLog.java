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

import com.google.caliper.bridge.BenchmarkModelLogMessage;
import com.google.caliper.bridge.DryRunSuccessLogMessage;
import com.google.caliper.bridge.FailureLogMessage;
import com.google.caliper.bridge.OpenedSocket;
import com.google.caliper.bridge.ShouldContinueMessage;
import com.google.caliper.bridge.StartMeasurementLogMessage;
import com.google.caliper.bridge.StartupAnnounceMessage;
import com.google.caliper.bridge.StopMeasurementLogMessage;
import com.google.caliper.bridge.VmPropertiesLogMessage;
import com.google.caliper.bridge.WorkerRequest;
import com.google.caliper.model.BenchmarkClassModel;
import com.google.caliper.model.Measurement;
import java.io.Closeable;
import java.io.IOException;
import java.io.Serializable;
import java.util.UUID;

/** The worker's interface for communicating with the runner. */
final class WorkerEventLog implements Closeable {
  private final OpenedSocket.Writer writer;
  private final OpenedSocket.Reader reader;

  WorkerEventLog(OpenedSocket socket) {
    this.writer = socket.writer();
    this.reader = socket.reader();
  }

  /**
   * Sends the runner a message telling it that this worker has started and then waits for the
   * runner to send this worker the request it should handle.
   */
  WorkerRequest notifyWorkerStarted(UUID id) throws IOException {
    send(new StartupAnnounceMessage(id));
    return (WorkerRequest) reader.read();
  }

  void notifyVmProperties() throws IOException {
    send(new VmPropertiesLogMessage());
  }

  void notifyTrialBootstrapPhaseStarting() throws IOException {
    send("Bootstrap phase starting.");
  }

  void notifyTrialMeasurementPhaseStarting() throws IOException {
    send("Measurement phase starting (includes warmup and actual measurement).");
  }

  void notifyTrialMeasurementStarting() throws IOException {
    send("About to measure.", new StartMeasurementLogMessage());
  }

  /**
   * Report the measurements and wait for it to be ack'd by the runner. Returns a message received
   * from the runner, which lets us know whether to continue measuring and whether we're in the
   * warmup or measurement phase.
   */
  ShouldContinueMessage notifyTrialMeasurementEnding(Iterable<Measurement> measurements)
      throws IOException {
    send(new StopMeasurementLogMessage(measurements));
    return (ShouldContinueMessage) reader.read();
  }

  void notifyDryRunSuccess(Iterable<Integer> ids) throws IOException {
    send(DryRunSuccessLogMessage.create(ids));
  }

  void sendBenchmarkModel(BenchmarkClassModel model) throws IOException {
    send(BenchmarkModelLogMessage.create(model));
  }

  void notifyFailure(Throwable e) throws IOException {
    send(FailureLogMessage.create(e));
  }

  /** Sends the given messages to the runner. */
  private void send(Serializable... messages) throws IOException {
    for (Serializable message : messages) {
      writer.write(message);
    }
    writer.flush();
  }

  @Override
  public void close() throws IOException {
    try {
      reader.close();
    } finally {
      writer.close();
    }
  }
}
