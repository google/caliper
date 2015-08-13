/*
 * Copyright (C) 2013 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */

package com.google.caliper.runner;

import static org.junit.Assert.assertEquals;

import com.google.caliper.BeforeExperiment;
import com.google.caliper.Benchmark;
import com.google.caliper.model.Measurement;
import com.google.caliper.model.Trial;
import com.google.common.collect.ImmutableListMultimap;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.io.File;
import java.util.List;

/**
 * Tests {@link AllocationInstrument}.
 */

@RunWith(JUnit4.class)
public class AllocationInstrumentTest {

  private final boolean LESS_THAN_JAVA_7 = System.getProperty("java.version").startsWith("1.5") || System.getProperty("java.version").startsWith("1.6");

  // (Java 1.7+) 14 objects and 1960 bytes are the known values for growing an ArrayList from 1 element to 100 elements
  private double objects = 14.0;
  private double bytes = 1960.0;
  {
    if (LESS_THAN_JAVA_7) {
        // (Java 1.5, 1.6)
        objects = 12.0;
        bytes = 1824.0;
    }
  }

  @Rule public CaliperTestWatcher runner = new CaliperTestWatcher();

  @Test public void getExtraCommandLineArgs() throws Exception {
    AllocationInstrument instrument = new AllocationInstrument();
    File fakeJar = File.createTempFile("fake", "jar");
    fakeJar.deleteOnExit();
    instrument.setOptions(ImmutableMap.of("allocationAgentJar", fakeJar.getAbsolutePath()));
    ImmutableSet<String> expected = new ImmutableSet.Builder<String>()
        .addAll(Instrument.JVM_ARGS)
        .add("-Xint")
        .add("-javaagent:" + fakeJar.getAbsolutePath())
        .add("-Xbootclasspath/a:" + fakeJar.getAbsolutePath())
        .add("-Dsun.reflect.inflationThreshold=0")
        .build();
    assertEquals(expected, instrument.getExtraCommandLineArgs());
    fakeJar.delete();
  }

  @Test
  public void intrinsics() throws Exception {
    runner.forBenchmark(ArrayListGrowthBenchmark.class)
        .instrument("allocation")
        .run();
    Trial trial = Iterables.getOnlyElement(runner.trials());
    ImmutableListMultimap<String, Measurement> measurementsByDescription = Measurement.indexByDescription(trial.measurements());
    for (Measurement objectMeasurement : measurementsByDescription.get("objects")) {
      assertEquals(objects, objectMeasurement.value().magnitude() / objectMeasurement.weight(), 0.001);
    }
    for (Measurement byteMeasurement : measurementsByDescription.get("bytes")) {
      assertEquals(bytes, byteMeasurement.value().magnitude() / byteMeasurement.weight(), 0.001);
    }
  }

  public static class TestBenchmark {
    List<Object> list = Lists.newLinkedList();
    @Benchmark public int compressionSize(int reps) {
      for (int i = 0; i < reps; i++) {
        list.add(new Object());
      }
      int hashCode = list.hashCode();
      list.clear();
      return hashCode;
    }
  }

  public static class ArrayListGrowthBenchmark {
    @BeforeExperiment void warmUp() {
      // ensure that hotspot has compiled this code
      benchmarkGrowth(100000);
    }

    @Benchmark void benchmarkGrowth(int reps) {
      for (int i = 0; i < reps; i++) {
        List<String> list = Lists.newArrayListWithCapacity(1);
        for (int j = 0; j < 100; j++) {
          list.add("");
        }
      }
    }
  }
}
