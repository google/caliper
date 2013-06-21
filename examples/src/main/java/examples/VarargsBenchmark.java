/*
 * Copyright (C) 2012 Google Inc.
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

import com.google.caliper.BeforeExperiment;
import com.google.caliper.Benchmark;
import com.google.caliper.Param;
import com.google.caliper.api.SkipThisScenarioException;

import java.util.Random;

/**
 * Benchmarks the overhead created by using varargs instead of parameter expansion.
 *
 * @author gak@google.com (Gregory Kick)
 */
public final class VarargsBenchmark {
  enum Strategy {
    VARARGS {
      @Override long one(long a) {
        return varargs(a);
      }

      @Override long two(long a, long b) {
        return varargs(a, b);
      }

      @Override long three(long a, long b, long c) {
        return varargs(a, b, c);
      }

      @Override long four(long a, long b, long c, long d) {
        return varargs(a, b, c, d);
      }

      @Override long five(long a, long b, long c, long d, long e) {
        return varargs(a, b, c, d);
      }

      @Override long six(long a, long b, long c, long d, long e, long f) {
        return varargs(a, b, c, d, e, f);
      }},
    EXPANSION {
      @Override long one(long a) {
        return VarargsBenchmark.one(a);
      }

      @Override long two(long a, long b) {
        return VarargsBenchmark.two(a, b);
      }

      @Override long three(long a, long b, long c) {
        return VarargsBenchmark.three(a, b, c);
      }

      @Override long four(long a, long b, long c, long d) {
        return VarargsBenchmark.four(a, b, c, d);
      }

      @Override long five(long a, long b, long c, long d, long e) {
        return VarargsBenchmark.five(a, b, c, d, e);
      }

      @Override long six(long a, long b, long c, long d, long e, long f) {
         return VarargsBenchmark.six(a, b, c, d, e, f);
      }
    };

    abstract long one(long a);

    abstract long two(long a, long b);

    abstract long three(long a, long b, long c);

    abstract long four(long a, long b, long c, long d);

    abstract long five(long a, long b, long c, long d, long e);

    abstract long six(long a, long b, long c, long d, long e, long f);
  }

  private static long varargs(long... longs) {
    long result = 0;
    for (long i : longs) {
      result ^= i;
    }
    return result;
  }

  private static long one(long a) {
    return a;
  }

  private static long two(long a, long b) {
    return a ^ b;
  }

  private static long three(long a, long b, long c) {
    return a ^ b ^ c;
  }

  private static long four(long a, long b, long c, long d) {
    return a ^ b ^ c ^ d;
  }

  private static long five(long a, long b, long c, long d, long e) {
    return a ^ b ^ c ^ d ^ e;
  }

  private static long six(long a, long b, long c, long d, long e, long f) {
    return a ^ b ^ c ^ d ^ e ^ f;
  }

  @Param private Strategy strategy;
  @Param({"1", "2", "3", "4", "5", "6"}) private int arguments;

  private long[] data = new long[2048];

  @BeforeExperiment void setUp() {
    Random random = new Random();
    for (int i = 0; i < data.length; i++) {
      data[i] = random.nextLong();
    }
  }

  @Benchmark long invocation(int reps) {
    switch (arguments) {
      case 1:
        return oneArgument(reps);
      case 2:
        return twoArguments(reps);
      case 3:
        return threeArguments(reps);
      case 4:
        return fourArguments(reps);
      case 5:
        return fiveArguments(reps);
      case 6:
        return sixArguments(reps);
      default:
        throw new SkipThisScenarioException();
    }
  }

  private long oneArgument(int reps) {
    long dummy = 0;
    long[] data = this.data;
    int dataLength = data.length;
    for (int i = 0; i < reps; i++) {
      dummy += strategy.one(data[i % dataLength]);
    }
    return dummy;
  }

  private long twoArguments(int reps) {
    long dummy = 0;
    long[] data = this.data;
    int dataLength = data.length;
    for (int i = 0; i < reps; i++) {
      dummy += strategy.two(data[i % dataLength], data[(i + 1) % dataLength]);
    }
    return dummy;
  }

  private long threeArguments(int reps) {
    long dummy = 0;
    long[] data = this.data;
    int dataLength = data.length;
    for (int i = 0; i < reps; i++) {
      dummy += strategy.three(
          data[i % dataLength],
          data[(i + 1) % dataLength],
          data[(i + 2) % dataLength]);
    }
    return dummy;
  }

  private long fourArguments(int reps) {
    long dummy = 0;
    long[] data = this.data;
    int dataLength = data.length;
    for (int i = 0; i < reps; i++) {
      dummy += strategy.four(
          data[i % dataLength],
          data[(i + 1) % dataLength],
          data[(i + 2) % dataLength],
          data[(i + 3) % dataLength]);
    }
    return dummy;
  }

  private long fiveArguments(int reps) {
    long dummy = 0;
    long[] data = this.data;
    int dataLength = data.length;
    for (int i = 0; i < reps; i++) {
      dummy += strategy.five(
          data[i % dataLength],
          data[(i + 1) % dataLength],
          data[(i + 2) % dataLength],
          data[(i + 3) % dataLength],
          data[(i + 4) % dataLength]);
    }
    return dummy;
  }

  private long sixArguments(int reps) {
    long dummy = 0;
    long[] data = this.data;
    int dataLength = data.length;
    for (int i = 0; i < reps; i++) {
      dummy += strategy.six(
          data[i % dataLength],
          data[(i + 1) % dataLength],
          data[(i + 2) % dataLength],
          data[(i + 3) % dataLength],
          data[(i + 4) % dataLength],
          data[(i + 5) % dataLength]);
    }
    return dummy;
  }
}
