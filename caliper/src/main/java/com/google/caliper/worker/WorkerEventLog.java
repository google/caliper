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

import com.google.caliper.bridge.CaliperControlLogMessage;
import com.google.caliper.bridge.FailureLogMessage;
import com.google.caliper.bridge.Renderer;
import com.google.caliper.bridge.StartMeasurementLogMessage;
import com.google.caliper.bridge.StopMeasurementLogMessage;
import com.google.caliper.bridge.VmPropertiesLogMessage;
import com.google.caliper.model.Measurement;
import com.google.common.collect.ImmutableList;
import com.google.inject.Inject;

import java.io.PrintWriter;

public final class WorkerEventLog {
  private final PrintWriter writer;
  private final Renderer<CaliperControlLogMessage> controlLogMessageRenderer;

  @Inject WorkerEventLog(PrintWriter writer,
      Renderer<CaliperControlLogMessage> controlLogMessageRenderer) {
    this.writer = writer;
    this.controlLogMessageRenderer = controlLogMessageRenderer;
  }

  public void notifyWorkerStarted() {
    writer.println(controlLogMessageRenderer.render(new VmPropertiesLogMessage()));
  }

  public void notifyWarmupPhaseStarting() {
    writer.println("Warmup starting.");
  }

  public void notifyMeasurementPhaseStarting() {
    writer.println("Measurement phase starting.");
  }

  public void notifyMeasurementStarting() {
    writer.println("About to measure.");
    writer.println(controlLogMessageRenderer.render(new StartMeasurementLogMessage()));
  }

  public void notifyMeasurementEnding(Measurement measurement) {
    notifyMeasurementEnding(ImmutableList.of(measurement));

  }

  public void notifyMeasurementEnding(Iterable<Measurement> measurements) {
    writer.println(controlLogMessageRenderer.render(new StopMeasurementLogMessage(measurements)));
    for (Measurement measurement : measurements) {
      writer.printf("I got a result! %s: %f%s%n", measurement.description(),
          measurement.value().magnitude() / measurement.weight(), measurement.value().unit());
    }
  }

  public void notifyMeasurementFailure(Exception e) {
    writer.println("MEASUREMENT FAILURE!");
    e.printStackTrace(writer);
  }

  public void notifyFailure(Exception e) {
    writer.println(controlLogMessageRenderer.render(new FailureLogMessage(e)));
  }
}
