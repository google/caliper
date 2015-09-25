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

package dk.ilios.caliperx.worker;

import dk.ilios.caliperx.bridge.FailureLogMessage;
import dk.ilios.caliperx.bridge.OpenedSocket;
import dk.ilios.caliperx.bridge.ShouldContinueMessage;
import dk.ilios.caliperx.bridge.StartMeasurementLogMessage;
import dk.ilios.caliperx.bridge.StartupAnnounceMessage;
import dk.ilios.caliperx.bridge.StopMeasurementLogMessage;
import dk.ilios.caliperx.bridge.VmPropertiesLogMessage;
import dk.ilios.caliperx.model.Measurement;
import com.google.inject.Inject;

import java.io.Closeable;
import java.io.IOException;
import java.util.UUID;

/** The worker's interface for communicating with the runner. */
final class WorkerEventLog implements Closeable {
  private final OpenedSocket.Writer writer;
  private final OpenedSocket.Reader reader;

  WorkerEventLog(OpenedSocket socket) {
    this.writer = socket.writer();
    this.reader = socket.reader();
  }

  void notifyWorkerStarted(UUID trialId) throws IOException {
    writer.write(new StartupAnnounceMessage(trialId));
    writer.write(new VmPropertiesLogMessage());
    writer.flush();
  }

  void notifyBootstrapPhaseStarting() throws IOException {
    writer.write("Bootstrap phase starting.");
    writer.flush();
  }

  void notifyMeasurementPhaseStarting() throws IOException {
    writer.write("Measurement phase starting (includes warmup and actual measurement).");
    writer.flush();
  }

  void notifyMeasurementStarting() throws IOException {
    writer.write("About to measure.");
    writer.write(new StartMeasurementLogMessage());
    writer.flush();
  }

  /**
   * Report the measurements and wait for it to be ack'd by the runner. Returns a message received
   * from the runner, which lets us know whether to continue measuring and whether we're in the
   * warmup or measurement phase.
   */
  ShouldContinueMessage notifyMeasurementEnding(Iterable<Measurement> measurements) throws
      IOException {
    writer.write(new StopMeasurementLogMessage(measurements));
    writer.flush();
    return (ShouldContinueMessage) reader.read();
  }

  void notifyFailure(Exception e) throws IOException {
    writer.write(new FailureLogMessage(e));
    writer.flush();
  }

  @Override public void close() throws IOException {
    try {
      reader.close();
    } finally {
      writer.close();
    }
  }
}
