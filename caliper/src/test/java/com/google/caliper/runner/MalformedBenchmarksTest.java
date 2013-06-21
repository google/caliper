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

package com.google.caliper.runner;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import com.google.caliper.Param;
import com.google.caliper.config.InvalidConfigurationException;
import com.google.caliper.legacy.Benchmark;
import com.google.caliper.util.InvalidCommandException;

import junit.framework.AssertionFailedError;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.io.PrintWriter;
import java.io.StringWriter;

/**
 * Unit test covering common user mistakes in benchmark classes.
 */
@RunWith(JUnit4.class)

public class MalformedBenchmarksTest {
  // Put the expected messages together here, which may promote some kind of
  // consistency in their wording. :)

  private static final String ABSTRACT =
      "Class '%s' is abstract";
  private static final String NO_CONSTRUCTOR =
      "Could not create an instance of the benchmark class following reasons:\n"
      + "  1) Could not find a suitable constructor in %s. "
      + "Classes must have either one (and only one) constructor annotated with @Inject "
      + "or a zero-argument constructor that is not private.";
  private static final String NO_METHODS =
      "There were no experiments to be performed for the class %s using the instruments " +
      "[allocation, runtime]";
  private static final String STATIC_BENCHMARK =
      "Benchmark methods must not be static: timeIt";
  private static final String WRONG_ARGUMENTS =
      "Benchmark methods must have no arguments or accept a single int or long parameter: timeIt";
  private static final String STATIC_PARAM =
      "Parameter field 'oops' must not be static";
  private static final String RESERVED_PARAM =
      "Class '%s' uses reserved parameter name 'vm'";
  private static final String NO_CONVERSION = "Type 'Object' of parameter field 'oops' "
      + "has no recognized String-converting method; see <TODO> for details";
  private static final String CONVERT_FAILED = // granted this one's a little weird (and brittle)
      "Cannot convert value 'oops' to type 'int': For input string: \"oops\"";

  @Test public void abstractBenchmark() throws Exception {
    expectException(ABSTRACT, AbstractBenchmark.class);
  }
  abstract static class AbstractBenchmark extends Benchmark {}

  @Test public void noSuitableConstructor() throws Exception {
    expectException(String.format(NO_CONSTRUCTOR, BadConstructorBenchmark.class.getName()),
        BadConstructorBenchmark.class);
  }
  static class BadConstructorBenchmark extends Benchmark {
    BadConstructorBenchmark(String damnParam) {}
    public void timeIt(int reps) {}
  }

  @Test public void noBenchmarkMethods() throws Exception {
    expectException(NO_METHODS, NoMethodsBenchmark.class);
  }
  static class NoMethodsBenchmark extends Benchmark {
    void timeIt(int reps) {} // not public
    public void timIt(int reps) {} // wrong name
  }

  @Test public void staticBenchmarkMethod() throws Exception {
    expectException(STATIC_BENCHMARK, StaticBenchmarkMethodBenchmark.class);
  }
  static class StaticBenchmarkMethodBenchmark extends Benchmark {
    public static void timeIt(int reps) {}
  }

  @Test public void wrongSignature() throws Exception {
    expectException(WRONG_ARGUMENTS, BoxedParamBenchmark.class);
    expectException(WRONG_ARGUMENTS, ExtraParamBenchmark.class);
  }
  static class BoxedParamBenchmark extends Benchmark {
    public void timeIt(Integer reps) {}
  }
  static class ExtraParamBenchmark extends Benchmark {
    public void timeIt(int reps, int what) {}
  }

  @Test public void hasBenchmarkOverloadsTimeMethods() throws Exception {
    expectException(
        "Overloads are disallowed for benchmark methods, found overloads of "
        + "[timeBar, timeBaz, timeFoo] in benchmark OverloadsBenchmark",
        OverloadsBenchmark.class);
  }

  static class OverloadsBenchmark extends Benchmark {
    public void timeFoo(long reps) {}
    public void timeFoo(int reps) {}
    public void timeBar(int reps) {}
    public void timeBar(long reps) {}
    public void timeBaz(long reps) {}
    public void timeBaz(long reps, boolean thing) {}
  }

  @Test public void hasBenchmarkOverloadsAnnotatedMethods() throws Exception {
    // N.B. baz is fine since although it has an overload, its overload is not a benchmark method.
    expectException(
        "Overloads are disallowed for benchmark methods, found overloads of [bar, foo] in "
        + "benchmark OverloadsAnnotatedBenchmark",
        OverloadsAnnotatedBenchmark.class);
  }

  static class OverloadsAnnotatedBenchmark extends Benchmark {
    @com.google.caliper.Benchmark public void foo(long reps) {}
    @com.google.caliper.Benchmark public void foo(int reps) {}
    @com.google.caliper.Benchmark public void bar(long reps) {}
    @com.google.caliper.Benchmark public void bar(int reps) {}
    @com.google.caliper.Benchmark public void baz(int reps) {}
    public void baz(long reps, boolean thing) {}
    public void baz(long reps) {}
  }

  @Test public void staticParam() throws Exception {
    expectException(STATIC_PARAM, StaticParamBenchmark.class);
  }
  static class StaticParamBenchmark extends Benchmark {
    @Param static String oops;
  }

  @Test public void reservedParameterName() throws Exception {
    expectException(RESERVED_PARAM, ReservedParamBenchmark.class);
  }
  static class ReservedParamBenchmark extends Benchmark {
    @Param String vm;
  }

  @Test public void unparsableParamType() throws Exception {
    expectException(NO_CONVERSION, UnparsableParamTypeBenchmark.class);
  }
  static class UnparsableParamTypeBenchmark extends Benchmark {
    @Param Object oops;
  }

  @Test public void unparsableParamDefault() throws Exception {
    expectException(CONVERT_FAILED, UnparsableParamDefaultBenchmark.class);
  }
  static class UnparsableParamDefaultBenchmark extends Benchmark {
    @Param({"1", "2", "oops"}) int number;
  }

  // end of tests

  private void expectException(String expectedMessageFmt, Class<?> benchmarkClass)
      throws InvalidCommandException, InvalidConfigurationException {
    try {
      CaliperMain.exitlessMain(
          new String[] {"--instrument=allocation,runtime", "--dry-run", benchmarkClass.getName()},
          new PrintWriter(new StringWriter()), new PrintWriter(new StringWriter()));
      fail("no exception thrown");
    } catch (InvalidBenchmarkException e) {
      try {
        String expectedMessageText =
            String.format(expectedMessageFmt, benchmarkClass.getSimpleName());
        assertEquals(expectedMessageText, e.getMessage());

        // don't swallow our real stack trace
      } catch (AssertionFailedError afe) {
        afe.initCause(e);
        throw afe;
      }
    }
  }
}
