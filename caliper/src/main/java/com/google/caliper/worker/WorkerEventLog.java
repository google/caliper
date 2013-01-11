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

import com.google.caliper.bridge.FailureLogMessage;
import com.google.caliper.bridge.Renderer;
import com.google.caliper.bridge.StartTimingLogMessage;
import com.google.caliper.bridge.StopTimingLogMessage;
import com.google.caliper.bridge.VmPropertiesLogMessage;
import com.google.caliper.model.Measurement;
import com.google.common.base.Predicate;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import com.google.inject.Inject;

import java.io.PrintWriter;
import java.util.Map;

public final class WorkerEventLog {
  private final PrintWriter writer;
  private final Renderer<VmPropertiesLogMessage> vmPropertiesRenderer;
  private final Renderer<StartTimingLogMessage> startTimingRenderer;
  private final Renderer<StopTimingLogMessage> stopTimingRenderer;
  private final Renderer<FailureLogMessage> failureRenderer;

  @Inject WorkerEventLog(PrintWriter writer,
      Renderer<VmPropertiesLogMessage> vmPropertiesRenderer,
      Renderer<StartTimingLogMessage> startTimingRenderer,
      Renderer<StopTimingLogMessage> stopTimingRenderer,
      Renderer<FailureLogMessage> failureRenderer) {
    this.writer = writer;
    this.vmPropertiesRenderer = vmPropertiesRenderer;
    this.startTimingRenderer = startTimingRenderer;
    this.stopTimingRenderer = stopTimingRenderer;
    this.failureRenderer = failureRenderer;
  }

  public void notifyWorkerStarted() {
    Map<String, String> vmProperties =
        Maps.filterKeys(Maps.fromProperties(System.getProperties()), new Predicate<String>() {
          @Override
          public boolean apply(String input) {
            return input.startsWith("java.vm")
                || input.startsWith("java.runtime")
                || input.equals("java.version")
                || input.equals("java.vendor");
          }
        });
    writer.println(vmPropertiesRenderer.render(
        new VmPropertiesLogMessage(ImmutableMap.copyOf(vmProperties))));
  }

  public void notifyWarmupPhaseStarting() {
    writer.println("Warmup starting.");
  }

  public void notifyMeasurementPhaseStarting() {
    writer.println("Measurement phase starting.");
  }

  public void notifyMeasurementStarting() {
    writer.println("About to measure.");
    writer.println(startTimingRenderer.render(new StartTimingLogMessage()));
  }

  public void notifyMeasurementEnding(Measurement measurement) {
    notifyMeasurementEnding(ImmutableList.of(measurement));

  }

  public void notifyMeasurementEnding(Iterable<Measurement> measurements) {
    writer.println(stopTimingRenderer.render(new StopTimingLogMessage(measurements)));
    for (Measurement measurement : measurements) {
      writer.println(String.format("I got a result! %s: %f%s", measurement.description(),
          measurement.value().magnitude() / measurement.weight(), measurement.value().unit()));
    }
  }

  public void notifyFailure(Exception e) {
    writer.println(failureRenderer.render(
        new FailureLogMessage(e.getClass().getName(), Strings.nullToEmpty(e.getMessage()),
            ImmutableList.copyOf(e.getStackTrace()))));
  }
}
