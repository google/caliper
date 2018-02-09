/*
 * Copyright (C) 2017 Google Inc.
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

import static com.google.common.truth.Truth.assertThat;
import static java.util.concurrent.TimeUnit.NANOSECONDS;

import com.google.caliper.Benchmark;
import com.google.caliper.model.BenchmarkClassModel.MethodModel;
import com.google.caliper.runner.config.VmConfig;
import com.google.caliper.runner.testing.FakePlatform;
import com.google.caliper.util.ShortDuration;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import java.io.File;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/** Tests {@link Experiment}. */
@RunWith(JUnit4.class)
public class ExperimentTest {

  @Test
  public void testToString() throws Exception {
    // The toString() of an instrument is used in console output to the user, so test it to prevent
    // unexpected changes to the format.
    Experiment experiment = createFakeExperiment();
    assertThat(experiment.toString())
        .isEqualTo(
            "{instrument=runtime, benchmarkMethod=myBenchmark, target=foo, parameters={baz=qux}}");
  }

  private static Experiment createFakeExperiment() throws Exception {
    RuntimeInstrument instrument = new RuntimeInstrument(ShortDuration.of(100, NANOSECONDS));
    instrument.setInstrumentName("runtime");
    MethodModel method =
        MethodModel.of(FooBenchmark.class.getDeclaredMethod("myBenchmark", long.class));
    return Experiment.create(
        1,
        instrument.createInstrumentedMethod(method),
        ImmutableMap.of("baz", "qux"),
        Target.create(
            "foo",
            new VmConfig(
                new File("foo"), ImmutableList.of(), new File("java"), new FakePlatform())));
  }

  static class FooBenchmark {
    @Benchmark
    public long myBenchmark(long reps) {
      return reps;
    }
  }
}
