/*
 * Copyright (C) 2009 Google Inc.
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

import com.google.caliper.SimpleBenchmark;
import com.google.caliper.Param;
import com.google.caliper.Runner;

import java.util.Arrays;
import java.util.Collection;

/**
 * Measures the various ways the JDK converts boxed Doubles to Strings.
 */
public class BoxedDoubleToStringBenchmark extends SimpleBenchmark {

  @Param private Double d;

  private static final Collection<Double> dValues = Arrays.asList(
      Math.PI,
      -0.0d,
      Double.NEGATIVE_INFINITY,
      Double.NaN
  );

  public void timeStringFormat(int reps) {
    Double value = d;
    for (int i = 0; i < reps; i++) {
      String.format("%f", value);
    }
  }

  public void timeToString(int reps) {
    Double value = d;
    for (int i = 0; i < reps; i++) {
      value.toString();
    }
  }

  public void timeStringValueOf(int reps) {
    Double value = d;
    for (int i = 0; i < reps; i++) {
      String.valueOf(value);
    }
  }

  public void timeQuoteTrick(int reps) {
    Double value = d;
    for (int i = 0; i < reps; i++) {
      String unused = ("" + value);
    }
  }

  // TODO: remove this from all examples when IDE plugins are ready
  public static void main(String[] args) throws Exception {
    Runner.main(BoxedDoubleToStringBenchmark.class, args);
  }
}
