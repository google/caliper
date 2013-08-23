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

package com.google.caliper.runner;

import static org.junit.Assert.assertEquals;

import com.google.caliper.model.ArbitraryMeasurement;
import com.google.caliper.model.Measurement;
import com.google.caliper.model.Value;
import com.google.common.collect.Iterables;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/**
 * Integration tests for the {@link ArbitraryMeasurementInstrument}
 */
@RunWith(JUnit4.class)
public class ArbitraryMeasurmentInstrumentTest {
  @Rule public CaliperTestWatcher runner = new CaliperTestWatcher();

  @Test

  public void testSuccess() throws Exception {
    runner.forBenchmark(TestBenchmark.class)
        .instrument("arbitrary")
        .run();
    Measurement measurement = Iterables.getOnlyElement(
        Iterables.getOnlyElement(runner.trials()).measurements());
    Measurement expected = new Measurement.Builder()
        .description("fake measurment")
        .weight(1)
        .value(Value.create(1.0, "hz"))
        .build();
    assertEquals(expected, measurement);
  }

  public static class TestBenchmark {
    @ArbitraryMeasurement(units = "hz", description = "fake measurment")
    public double compressionSize() {
      return 1.0;
    }
  }
}
