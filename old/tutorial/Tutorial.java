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

package tutorial;

import com.google.caliper.BeforeExperiment;
import com.google.caliper.Benchmark;
import com.google.caliper.Param;

/**
 * Caliper tutorial. To run the example benchmarks in this file:
 * {@code CLASSPATH=... [caliper_home]/caliper tutorial.Tutorial.Benchmark1}
 */
public class Tutorial {

  /*
   * We begin the Caliper tutorial with the simplest benchmark you can write.
   * We'd like to know how efficient the method System.nanoTime() is.
   *
   * Notice:
   *
   *  - We write a class that extends com.google.caliper.Benchmark.
   *  - It contains a public instance method whose name begins with 'time' and
   *    which accepts a single 'int reps' parameter.
   *  - The body of the method simply executes the code we wish to measure,
   *    'reps' times.
   *
   * Example run:
   *
   *    $ CLASSPATH=build/classes/test caliper tutorial.Tutorial.Benchmark1
   *    [real-time results appear on this line]
   *
   *    Summary report for tutorial.Tutorial$Benchmark1:
   *
   *    Benchmark   ns
   *    ---------  ---
   *    NanoTime   233
   */
  public static class Benchmark1 {
    @Benchmark void timeNanoTime(int reps) {
      for (int i = 0; i < reps; i++) {
        System.nanoTime();
      }
    }
  }

  /*
   * Now let's compare two things: nanoTime() versus currentTimeMillis().
   * Notice:
   *
   *  - We simply add another method, following the same rules as the first.
   *
   * Example run output:
   *
   *   Benchmark           ns
   *   -----------------  ---
   *   NanoTime           248
   *   CurrentTimeMillis  118
   */
  public static class Benchmark2 {
    @Benchmark void timeNanoTime(int reps) {
      for (int i = 0; i < reps; i++) {
        System.nanoTime();
      }
    }
    @Benchmark void timeCurrentTimeMillis(int reps) {
      for (int i = 0; i < reps; i++) {
        System.currentTimeMillis();
      }
    }
  }

  /*
   * Let's try iterating over a large array. This seems simple enough, but
   * there is a problem!
   */
  public static class Benchmark3 {
    private final int[] array = new int[1000000];

    @SuppressWarnings("UnusedDeclaration") // IDEA tries to warn us!
    @Benchmark void timeArrayIteration_BAD(int reps) {
      for (int i = 0; i < reps; i++) {
        for (int ignoreMe : array) {}
      }
    }
  }

  /*
   * Caliper reported that the benchmark above ran in 4 nanoseconds.
   *
   * Wait, what?
   *
   * How can it possibly iterate over a million zeroes in 4 ns!?
   *
   * It is very important to sanity-check benchmark results with common sense!
   * In this case, we're indeed getting a bogus result. The problem is that the
   * Java Virtual Machine is too smart: it detected the fact that the loop was
   * producing no actual result, so it simply compiled it right out. The method
   * never looped at all. To fix this, we need to use a dummy result value.
   *
   * Notice:
   *
   *  - We simply change the 'time' method from 'void' to any return type we
   *    wish. Then we return a value that can't be known without actually
   *    performing the work, and thus we defeat the runtime optimizations.
   *  - We're no longer timing *just* the code we want to be testing - our
   *    result will now be inflated by the (small) cost of addition. This is an
   *    unfortunate fact of life with microbenchmarking. In fact, we were
   *    already inflated by the cost of an int comparison, "i < reps" as it was.
   *
   * With this change, Caliper should report a much more realistic value, more
   * on the order of an entire millisecond.
   */
  public static class Benchmark4 {
    private final int[] array = new int[1000000];

    @Benchmark int timeArrayIteration_fixed(int reps) {
      int dummy = 0;
      for (int i = 0; i < reps; i++) {
        for (int doNotIgnoreMe : array) {
          dummy += doNotIgnoreMe;
        }
      }
      return dummy; // framework ignores this, but it has served its purpose!
    }
  }

  /*
   * Now we'd like to know how various other *sizes* of arrays perform. We
   * don't want to have to cut and paste the whole benchmark just to provide a
   * different size. What we need is a parameter!
   *
   * When you run this benchmark the same way you ran the previous ones, you'll
   * now get an error: "No values provided for benchmark parameter 'size'".
   * You can provide the value requested at the command line like this:
   *
   *   [caliper_home]/caliper tutorial.Tutorial.Benchmark5 -Dsize=100}
   *
   * You'll see output like this:
   *
   *   Benchmark       size   ns
   *   --------------  ----  ---
   *   ArrayIteration   100   51
   *
   * Now that we've parameterized our benchmark, things are starting to get fun.
   * Try passing '-Dsize=10,100,1000' and see what happens!
   *
   *   Benchmark       size   ns
   *   --------------  ----  -----------------------------------
   *   ArrayIteration    10    7 |
   *   ArrayIteration   100   49 ||||
   *   ArrayIteration  1000  477 ||||||||||||||||||||||||||||||
   *
   */
  public static class Benchmark5 {
    @Param int size; // set automatically by framework

    private int[] array; // set by us, in setUp()

    @BeforeExperiment void setUp() {
      // @Param values are guaranteed to have been injected by now
      array = new int[size];
    }

    @Benchmark int timeArrayIteration(int reps) {
      int dummy = 0;
      for (int i = 0; i < reps; i++) {
        for (int doNotIgnoreMe : array) {
          dummy += doNotIgnoreMe;
        }
      }
      return dummy;
    }
  }
}
