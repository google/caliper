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
import com.google.caliper.bridge.ShouldContinueMessage;
import com.google.caliper.bridge.StartMeasurementLogMessage;
import com.google.caliper.bridge.StartupAnnounceMessage;
import com.google.caliper.bridge.StopMeasurementLogMessage;
import com.google.caliper.bridge.VmPropertiesLogMessage;
import com.google.caliper.model.Measurement;
import com.google.common.collect.ImmutableList;
import com.google.gson.Gson;
import com.google.inject.Inject;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.util.UUID;

public final class WorkerEventLog {
  private final BufferedWriter writer;
  private final BufferedReader reader;
  private final Renderer<CaliperControlLogMessage> controlLogMessageRenderer;
  private final Gson parser;

  @Inject WorkerEventLog(BufferedWriter writer, BufferedReader reader,
      Renderer<CaliperControlLogMessage> controlLogMessageRenderer, Gson parser) {
    this.writer = writer;
    this.reader = reader;
    this.controlLogMessageRenderer = controlLogMessageRenderer;
    this.parser = parser;
  }

  public void notifyWorkerStarted(UUID trialId) throws IOException {
    println(parser.toJson(new StartupAnnounceMessage(trialId)));
    println(controlLogMessageRenderer.render(new VmPropertiesLogMessage()));
    writer.flush();
  }

  public void notifyWarmupPhaseStarting() throws IOException {
    printlnAndFlush("Warmup starting.");
  }

  public void notifyMeasurementPhaseStarting() throws IOException {
    printlnAndFlush("Measurement phase starting.");
  }

  public void notifyMeasurementStarting() throws IOException {
    println("About to measure.");
    println(controlLogMessageRenderer.render(new StartMeasurementLogMessage()));
    writer.flush();
  }

  /**
   * Report the measurement and wait for it to be ack'd by the runner.  Returns true if we should
   * keep measuring, false otherwise.
   */
  public boolean notifyMeasurementEnding(Measurement measurement) throws IOException {
    return notifyMeasurementEnding(ImmutableList.of(measurement));
  }

  /**
   * Report the measurements and wait for it to be ack'd by the runner.  Returns true if we should
   * keep measuring, false otherwise.
   */
  public boolean notifyMeasurementEnding(Iterable<Measurement> measurements) throws IOException {
    println(controlLogMessageRenderer.render(new StopMeasurementLogMessage(measurements)));
    for (Measurement measurement : measurements) {
      println(String.format("I got a result! %s: %f%s%n", measurement.description(),
          measurement.value().magnitude() / measurement.weight(), measurement.value().unit()));
    }
    writer.flush();
    return shouldKeepMeasuring();
  }

  public void notifyFailure(Exception e) throws IOException {
    printlnAndFlush(controlLogMessageRenderer.render(new FailureLogMessage(e)));
  }
  
  private boolean shouldKeepMeasuring() throws IOException {
    return parser.fromJson(reader.readLine(), ShouldContinueMessage.class).shouldContinue();
  }
  
  private void println(String str) throws IOException {
    writer.write(str);
    writer.write('\n');
  }
  
  private void printlnAndFlush(String str) throws IOException {
    println(str);
    writer.flush();
  }
}
