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

/**
 * Measures the various ways the JDK converts doubles to strings.
 */
public class DoubleToStringBenchmark2 extends SimpleBenchmark {
  @Param boolean useWrapper;

  enum Value {
    Pi(Math.PI),
    NegativeZero(-0.0),
    NegativeInfinity(Double.NEGATIVE_INFINITY),
    NaN(Double.NaN);

    final double d;

    Value(double d) {
      this.d = d;
    }
  }

  @Param Value value;

  public int timeToString(int reps) {
    int dummy = 0;
    if (useWrapper) {
      Double d = value.d;
      for (int i = 0; i < reps; i++) {
        dummy += d.toString().length();
      }
    } else {
      double d = value.d;
      for (int i = 0; i < reps; i++) {
        dummy += ((Double) d).toString().length();
      }
    }
    return dummy;
  }

  public int timeStringValueOf(int reps) {
    int dummy = 0;
    if (useWrapper) {
      Double d = value.d;
      for (int i = 0; i < reps; i++) {
        dummy += String.valueOf(d).length();
      }
    } else {
      double d = value.d;
      for (int i = 0; i < reps; i++) {
        dummy += String.valueOf(d).length();
      }
    }
    return dummy;
  }

  public int timeStringFormat(int reps) {
    int dummy = 0;
    if (useWrapper) {
      Double d = value.d;
      for (int i = 0; i < reps; i++) {
        dummy += String.format("%f", d).length();
      }
    } else {
      double d = value.d;
      for (int i = 0; i < reps; i++) {
        dummy += String.format("%f", d).length();
      }
    }
    return dummy;
  }

  public int timeQuoteTrick(int reps) {
    int dummy = 0;
    if (useWrapper) {
      Double d = value.d;
      for (int i = 0; i < reps; i++) {
        dummy += ("" + d).length();
      }
    } else {
      double d = value.d;
      for (int i = 0; i < reps; i++) {
        dummy += ("" + d).length();
      }
    }
    return dummy;
  }

  public static void main(String[] args) throws Exception {
    Runner.main(DoubleToStringBenchmark2.class, args);
  }
}
