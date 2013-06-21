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

package examples;

import com.google.caliper.AfterExperiment;
import com.google.caliper.BeforeExperiment;
import com.google.caliper.Benchmark;
import com.google.caliper.Param;
import com.google.caliper.api.SkipThisScenarioException;
import com.google.caliper.api.VmOptions;
import com.google.caliper.util.ShortDuration;

import java.math.BigDecimal;

@VmOptions("-server")
public class DemoBenchmark {
  @Param({"abc", "def", "xyz"}) String string;
  @Param({"1", "2"}) int number;
  @Param Foo foo;

  @Param({"0.00", "123.45"}) BigDecimal money;
  @Param({"1ns", "2 minutes"}) ShortDuration duration;

  enum Foo {
    FOO, BAR, BAZ, QUX;
  }

  DemoBenchmark() {
//    System.out.println("I should not do this.");
  }

  @BeforeExperiment void setUp() throws Exception {
    if (string.equals("abc") && number == 1) {
      throw new SkipThisScenarioException();
    }
  }

  @Benchmark int something(int reps) {
    int dummy = 0;
    for (int i = 0; i < reps; i++) {
      dummy += i;
    }
    return dummy;
  }

  @Benchmark int somethingElse(int reps) {
    int dummy = 0;
    for (int i = 0; i < reps; i++) {
      dummy -= i;
    }
    return dummy;
  }

  @AfterExperiment void tearDown() throws Exception {
//    System.out.println("Hey, I'm tearing up the joint.");
  }
}
