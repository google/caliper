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

  private static final String DOESNT_EXTEND =
      "Class '%%s' does not extend %s";
  private static final String ABSTRACT =
      "Class '%s' is abstract";
  private static final String NO_CONSTRUCTOR =
      "Class '%s' has no parameterless constructor";
  private static final String NO_METHODS =
      "There were no experiments to be peformed for the class %s using the instruments " +
      "[allocation, micro]";
  private static final String STATIC_BENCHMARK =
      "Microbenchmark methods must not be static: timeIt";
  private static final String WRONG_ARGUMENTS =
      "Microbenchmark methods must accept a single int parameter: timeIt";
  private static final String STATIC_PARAM =
      "Parameter field 'oops' must not be static";
  private static final String RESERVED_PARAM =
      "Class '%s' uses reserved parameter name 'vm'";
  private static final String NO_CONVERSION = "Type 'Object' of parameter field 'oops' "
      + "has no recognized String-converting method; see <TODO> for details";
  private static final String CONVERT_FAILED = // granted this one's a little weird (and brittle)
      "Cannot convert value 'oops' to type 'int': For input string: \"oops\"";

  public void testDoesntExtendBenchmark() throws Exception {
    String expected = String.format(DOESNT_EXTEND, Benchmark.class.getName());
    expectException(expected, NotABenchmark.class);
  }
  static class NotABenchmark {}

  @Test public void abstractBenchmark() throws Exception {
    expectException(ABSTRACT, AbstractBenchmark.class);
  }
  abstract static class AbstractBenchmark extends Benchmark {}

  @Test public void noSuitableConstructor() throws Exception {
    expectException(NO_CONSTRUCTOR, BadConstructorBenchmark.class);
  }
  static class BadConstructorBenchmark extends Benchmark {
    BadConstructorBenchmark(String damnParam) {}
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
    expectException(WRONG_ARGUMENTS, WrongSignatureBenchmark1.class);
    expectException(WRONG_ARGUMENTS, WrongSignatureBenchmark2.class);
    expectException(WRONG_ARGUMENTS, WrongSignatureBenchmark3.class);
  }
  static class WrongSignatureBenchmark1 extends Benchmark {
    public void timeIt() {}
  }
  static class WrongSignatureBenchmark2 extends Benchmark {
    public void timeIt(Integer reps) {}
  }
  static class WrongSignatureBenchmark3 extends Benchmark {
    public void timeIt(int reps, int what) {}
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
          new String[] {"--instrument=allocation,micro", benchmarkClass.getName()},
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
