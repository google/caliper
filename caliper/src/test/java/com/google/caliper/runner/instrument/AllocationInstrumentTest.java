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

package com.google.caliper.runner.instrument;

import static org.junit.Assert.assertEquals;

import com.google.caliper.AfterExperiment;
import com.google.caliper.BeforeExperiment;
import com.google.caliper.Benchmark;
import com.google.caliper.api.AfterRep;
import com.google.caliper.api.BeforeRep;
import com.google.caliper.model.Measurement;
import com.google.caliper.model.Trial;
import com.google.caliper.runner.config.VmConfig;
import com.google.caliper.runner.config.VmType;
import com.google.caliper.runner.testing.CaliperTestWatcher;
import com.google.common.collect.ImmutableListMultimap;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import java.io.File;
import java.util.List;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/** Tests {@link AllocationInstrument}. */

@RunWith(JUnit4.class)
public class AllocationInstrumentTest {

  @Rule public final CaliperTestWatcher runner = new CaliperTestWatcher();

  @Test
  public void getExtraCommandLineArgs() throws Exception {
    AllocationInstrument instrument = new AllocationInstrument();
    File fakeJar = File.createTempFile("fake", "jar");
    fakeJar.deleteOnExit();
    instrument.setOptions(ImmutableMap.of("allocationAgentJar", fakeJar.getAbsolutePath()));
    ImmutableSet<String> expected =
        ImmutableSet.of(
            "-Xint",
            "-javaagent:" + fakeJar.getAbsolutePath(),
            "-Xbootclasspath/a:" + fakeJar.getAbsolutePath());
    VmConfig vmConfig =
        VmConfig.builder()
            .name("foo")
            .type(VmType.JVM)
            .home(System.getProperty("java.home"))
            .build();
    assertEquals(expected, instrument.getExtraCommandLineArgs(vmConfig));
    fakeJar.delete();
  }

  @Test
  public void intrinsics() throws Exception {
    runner.forBenchmark(ArrayListGrowthBenchmark.class).instrument("allocation").run();
    Trial trial = Iterables.getOnlyElement(runner.trials());
    ImmutableListMultimap<String, Measurement> measurementsByDescription =
        Measurement.indexByDescription(trial.measurements());
    // 14 objects and 1960 bytes are the known values for growing an ArrayList from 1 element to 100
    // elements
    for (Measurement objectMeasurement : measurementsByDescription.get("objects")) {
      assertEquals(14.0, objectMeasurement.value().magnitude() / objectMeasurement.weight(), 0.001);
    }
    for (Measurement byteMeasurement : measurementsByDescription.get("bytes")) {
      assertEquals(1960.0, byteMeasurement.value().magnitude() / byteMeasurement.weight(), 0.001);
    }
  }

  public static class TestBenchmark {
    @Benchmark
    public Integer compressionSize(int reps) {
      int result = 0;
      for (int i = 0; i < reps; i++) {
        result ^= new Object().hashCode();
      }
      /*
       * Force a new Integer instance to be allocated, since our call to this method is going to
       * require boxing even if our return type is int (because we call it reflectively). If we
       * don't call `new Integer`, Java will automatically call `Integer.valueOf`, which might
       * return a cached instance or might not. Caching depends on the values of the hash codes,
       * which may vary from JVM version to JVM version or even from run to run (especially given
       * that the number of reps is random).
       *
       * We could instead just return a boolean, since all of those are cached. But we're sweeping a
       * problem under the rug, so I'd rather be explicit about it.
       *
       * But... somehow this allocation is not visible to Caliper :( But at least it's consistently
       * not visible under both JDK8 and JDK9. That's an improvement over the old behavior, in which
       * the `Integer.valueOf` allocation (which I believe *was* always happening in practice) was
       * visible to JDK9 but not JDK8.
       */
      return new Integer(result);
    }
  }

  public static class TestMacroBenchmark {
    int beforeRepCalls = 0;
    int afterRepCalls = 0;
    int repCalls = 0;
    int hashCode = 0;

    @BeforeRep
    public void beforeRep() {
      beforeRepCalls++;
    }

    @Benchmark
    public void benchmark() {
      repCalls++;
      assertEquals(beforeRepCalls, afterRepCalls + 1);
      assertEquals(beforeRepCalls, repCalls);
      Object o = new Object();
      hashCode = o.hashCode();
    }

    @AfterRep
    public void afterRep() {
      afterRepCalls++;
    }

    @AfterExperiment
    public void afterExperiment() {
      assertEquals(repCalls, afterRepCalls);
      assertEquals(afterRepCalls, beforeRepCalls);
    }
  }

  public static class ArrayListGrowthBenchmark {
    @BeforeExperiment
    void warmUp() {
      // ensure that hotspot has compiled this code
      benchmarkGrowth(100000);
    }

    @Benchmark
    void benchmarkGrowth(int reps) {
      for (int i = 0; i < reps; i++) {
        List<String> list = Lists.newArrayListWithCapacity(1);
        for (int j = 0; j < 100; j++) {
          list.add("");
        }
      }
    }
  }
}
