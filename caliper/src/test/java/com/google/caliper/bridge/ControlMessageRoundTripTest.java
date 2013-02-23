/*
 * Copyright (C) 2013 Google Inc.
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

package com.google.caliper.bridge;

import static org.junit.Assert.assertEquals;

import com.google.caliper.json.GsonModule;
import com.google.caliper.model.Measurement;
import com.google.caliper.model.Value;
import com.google.common.collect.ImmutableList;
import com.google.inject.Guice;
import com.google.inject.Inject;
import com.google.inject.Injector;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/**
 * Tests the full round trip between {@link LogMessageParser} and {@link ControlLogMessageRenderer}.
 */
@RunWith(JUnit4.class)

public class ControlMessageRoundTripTest {
  @Inject LogMessageParser parser;
  @Inject ControlLogMessageRenderer renderer;

  @Before public void setUp() {
    Injector injector = Guice.createInjector(new GsonModule(), new BridgeModule());
    injector.injectMembers(this);
  }

  @Test public void failureLogMessage() {
    FailureLogMessage message = new FailureLogMessage(new IllegalArgumentException("my message"));
    assertEquals(message, parser.parse(renderer.render(message)));
  }

  @Test public void startMeasurementLogMessage() {
    StartMeasurementLogMessage message = new StartMeasurementLogMessage();
    assertEquals(message, parser.parse(renderer.render(message)));
  }

  @Test public void stopMeasurementLogMessage() {
    StopMeasurementLogMessage message = new StopMeasurementLogMessage(ImmutableList.of(
        new Measurement.Builder()
            .description("runtime")
            .weight(2.0)
            .value(Value.create(5.0, "ns"))
            .build()));
    assertEquals(message, parser.parse(renderer.render(message)));
  }

  @Test public void vmPropertiesLogMessage() {
    VmPropertiesLogMessage message = new VmPropertiesLogMessage();
    assertEquals(message, parser.parse(renderer.render(message)));
  }
}
