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

import com.google.caliper.Param;
import com.google.caliper.api.Benchmark;
import com.google.caliper.api.Launcher;
import com.google.caliper.api.SkipThisScenarioException;
import com.google.caliper.api.VmParam;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class DemoBenchmark extends Benchmark {
  @Param({"foo", "bar", "qux"}) String string;
  @Param({"1", "2"}) int number;
  @Param
  TimeUnit timeUnit;

  @Param BigDecimal money;
  static List<BigDecimal> moneyValues() {
    return Arrays.asList(new BigDecimal("123.45"), new BigDecimal("0.00"));
  }

  @VmParam({"-Xmx32m", "-Xmx1g"}) String memoryMax;

  DemoBenchmark() {
//    System.out.println("I should not do this.");
  }

  @Override public void setUp() throws Exception {
//    System.out.println("Hey, I'm setting up.");
    if (string.equals("foo") && number == 1) {
      throw new SkipThisScenarioException();
    }
  }

  public int timeSomething(int reps) {
    int dummy = 0;
    for (int i = 0; i < reps; i++) {
      dummy += i;
    }
    return dummy;
  }

  public int timeSomethingElse(int reps) {
    int dummy = 0;
    for (int i = 0; i < reps; i++) {
      dummy -= i;
    }
    return dummy;
  }

  @Override public void tearDown() throws Exception {
//    System.out.println("Hey, I'm tearing up the joint.");
  }

  public static void main(String[] args) {
    Launcher.launch(DemoBenchmark.class, args);
  }
}
