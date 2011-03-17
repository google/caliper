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

import com.google.caliper.Param;
import com.google.caliper.Runner;
import com.google.caliper.SimpleBenchmark;

import java.util.Arrays;
import java.util.List;

/**
 * Measures the various ways the JDK converts doubles to strings.
 */
public class DoubleToStringBenchmark2 extends SimpleBenchmark {
  @Param boolean useWrapper;

  @Param double value;

  public static final List<Double> valueValues = Arrays.asList(
      Math.PI,
      -0.0,
      Double.NEGATIVE_INFINITY,
      Double.NaN
  );

  Double wrapped;

  @Override public void setUp() {
    if (useWrapper) {
      wrapped = value;
    }
  }

  public int timeToString(int reps) {
    int dummy = 0;
    if (useWrapper) {
      for (int i = 0; i < reps; i++) {
        dummy += wrapped.toString().length();
      }
    } else {
      for (int i = 0; i < reps; i++) {
        dummy += ((Double) value).toString().length();
      }
    }
    return dummy;
  }

  public int timeStringValueOf(int reps) {
    int dummy = 0;
    if (useWrapper) {
      for (int i = 0; i < reps; i++) {
        dummy += String.valueOf(wrapped).length();
      }
    } else {
      for (int i = 0; i < reps; i++) {
        dummy += String.valueOf(value).length();
      }
    }
    return dummy;
  }

  public int timeStringFormat(int reps) {
    int dummy = 0;
    if (useWrapper) {
      for (int i = 0; i < reps; i++) {
        dummy += String.format("%f", wrapped).length();
      }
    } else {
      for (int i = 0; i < reps; i++) {
        dummy += String.format("%f", value).length();
      }
    }
    return dummy;
  }

  public int timeQuoteTrick(int reps) {
    int dummy = 0;
    if (useWrapper) {
      for (int i = 0; i < reps; i++) {
        dummy += ("" + wrapped).length();
      }
    } else {
      for (int i = 0; i < reps; i++) {
        dummy += ("" + value).length();
      }
    }
    return dummy;
  }

  public static void main(String[] args) throws Exception {
    Runner.main(DoubleToStringBenchmark2.class, args);
  }
}
