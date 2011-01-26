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
public class DoubleToStringBenchmark extends SimpleBenchmark {
  @Param Method method;

  public enum Method {
    TO_STRING {
      @Override String convert(double d) {
        return ((Double) d).toString();
      }
      @Override String convert(Double d) {
        return d.toString();
      }
    },
    STRING_VALUE_OF {
      @Override String convert(double d) {
        return String.valueOf(d);
      }
      @Override String convert(Double d) {
        return String.valueOf(d);
      }
    },
    STRING_FORMAT {
      @Override String convert(double d) {
        return String.format("%f", d);
      }
      @Override String convert(Double d) {
        return String.format("%f", d);
      }
    },
    QUOTE_TRICK {
      @Override String convert(double d) {
        return "" + d;
      }
      @Override String convert(Double d) {
        return "" + d;
      }
    },
    ;

    abstract String convert(double d);
    abstract String convert(Double d);
  }

  @Param double value;

  public static final List<Double> valueValues = Arrays.asList(
      Math.PI,
      -0.0,
      Double.NEGATIVE_INFINITY,
      Double.NaN
  );

  public int timePrimitive(int reps) {
    double d = value;
    int dummy = 0;
    for (int i = 0; i < reps; i++) {
      dummy += method.convert(d).length();
    }
    return dummy;
  }

  public int timeWrapper(int reps) {
    Double d = value;
    int dummy = 0;
    for (int i = 0; i < reps; i++) {
      dummy += method.convert(d).length();
    }
    return dummy;
  }

  public static void main(String[] args) throws Exception {
    Runner.main(DoubleToStringBenchmark.class, args);
  }
}
