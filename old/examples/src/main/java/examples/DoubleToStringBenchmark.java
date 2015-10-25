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

import com.google.caliper.Benchmark;
import com.google.caliper.Param;

/**
 * Measures the various ways the JDK converts doubles to strings.
 */
public class DoubleToStringBenchmark {
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

  enum Value {
    Pi(Math.PI),
    NegativeZero(-0.0),
    NegativeInfinity(Double.NEGATIVE_INFINITY),
    NaN(Double.NaN);

    final double value;

    Value(double value) {
      this.value = value;
    }
  }

  @Param Value value;

  @Benchmark int primitive(int reps) {
    double d = value.value;
    int dummy = 0;
    for (int i = 0; i < reps; i++) {
      dummy += method.convert(d).length();
    }
    return dummy;
  }

  @Benchmark int wrapper(int reps) {
    Double d = value.value;
    int dummy = 0;
    for (int i = 0; i < reps; i++) {
      dummy += method.convert(d).length();
    }
    return dummy;
  }
}
