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

import com.google.caliper.Benchmark;
import com.google.caliper.Param;
import com.google.caliper.config.InvalidConfigurationException;
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
      "Benchmark class %s does not have a publicly visible default constructor";
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
  abstract static class AbstractBenchmark {}

  @Test public void noSuitableConstructor() throws Exception {
    expectException(String.format(NO_CONSTRUCTOR, BadConstructorBenchmark.class.getName()),
        BadConstructorBenchmark.class);
  }

  @SuppressWarnings("unused")
  static class BadConstructorBenchmark {
    BadConstructorBenchmark(String damnParam) {}
    @Benchmark void timeIt(int reps) {}
  }

  @Test public void noBenchmarkMethods() throws Exception {
    expectException(NO_METHODS, NoMethodsBenchmark.class);
  }

  @SuppressWarnings("unused")
  static class NoMethodsBenchmark {
    void timeIt(int reps) {} // not annotated
  }

  @Test public void staticBenchmarkMethod() throws Exception {
    expectException(STATIC_BENCHMARK, StaticBenchmarkMethodBenchmark.class);
  }

  @SuppressWarnings("unused")
  static class StaticBenchmarkMethodBenchmark {
    @Benchmark public static void timeIt(int reps) {}
  }

  @Test public void wrongSignature() throws Exception {
    expectException(WRONG_ARGUMENTS, BoxedParamBenchmark.class);
    expectException(WRONG_ARGUMENTS, ExtraParamBenchmark.class);
  }

  @SuppressWarnings("unused")
  static class BoxedParamBenchmark {
    @Benchmark void timeIt(Integer reps) {}
  }

  @SuppressWarnings("unused")
  static class ExtraParamBenchmark {
    @Benchmark void timeIt(int reps, int what) {}
  }

  @Test public void hasBenchmarkOverloads() throws Exception {
    // N.B. baz is fine since although it has an overload, its overload is not a benchmark method.
    expectException(
        "Overloads are disallowed for benchmark methods, found overloads of [bar, foo] in "
        + "benchmark OverloadsAnnotatedBenchmark",
        OverloadsAnnotatedBenchmark.class);
  }

  @SuppressWarnings("unused")
  static class OverloadsAnnotatedBenchmark {
    @Benchmark public void foo(long reps) {}
    @Benchmark public void foo(int reps) {}
    @Benchmark public void bar(long reps) {}
    @Benchmark public void bar(int reps) {}
    @Benchmark public void baz(int reps) {}
    public void baz(long reps, boolean thing) {}
    public void baz(long reps) {}
  }

  @Test public void staticParam() throws Exception {
    expectException(STATIC_PARAM, StaticParamBenchmark.class);
  }
  static class StaticParamBenchmark {
    @Param static String oops;
  }

  @Test public void reservedParameterName() throws Exception {
    expectException(RESERVED_PARAM, ReservedParamBenchmark.class);
  }
  static class ReservedParamBenchmark {
    @Param String vm;
  }

  @Test public void unparsableParamType() throws Exception {
    expectException(NO_CONVERSION, UnparsableParamTypeBenchmark.class);
  }
  static class UnparsableParamTypeBenchmark {
    @Param Object oops;
  }

  @Test public void unparsableParamDefault() throws Exception {
    expectException(CONVERT_FAILED, UnparsableParamDefaultBenchmark.class);
  }
  static class UnparsableParamDefaultBenchmark {
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
