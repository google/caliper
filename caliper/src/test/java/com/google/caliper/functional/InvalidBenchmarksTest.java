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

package com.google.caliper.functional;

import com.google.caliper.Benchmark;
import com.google.caliper.Param;
import com.google.caliper.options.CaliperOptions;
import com.google.caliper.runner.BenchmarkClass;
import com.google.caliper.runner.ExperimentingCaliperRun;
import com.google.caliper.runner.InvalidBenchmarkException;
import com.google.caliper.runner.MicrobenchmarkInstrument;
import com.google.caliper.util.InvalidCommandException;
import com.google.common.collect.ImmutableMap;

import junit.framework.Assert;
import junit.framework.AssertionFailedError;

/**
 * Unit test covering common user mistakes in benchmark classes.
 */
public class InvalidBenchmarksTest extends Assert /* temporarily not a TestCase */ {
  // Put the expected messages together here, which may promote some kind of
  // consistency in their wording. :)

  static final String DOESNT_EXTEND =
      "Class '%%s' does not extend %s";
  static final String ABSTRACT =
      "Class '%s' is abstract";
  static final String NO_CONSTRUCTOR =
      "Class '%s' has no parameterless constructor";
  static final String NO_METHODS =
      "Class '%s' contains no benchmark methods for instrument 'micro'";
  static final String STATIC_BENCHMARK =
      "Microbenchmark methods must not be static: timeIt";
  static final String WRONG_ARGUMENTS =
      "Microbenchmark methods must accept a single int parameter: timeIt";
  static final String STATIC_PARAM =
      "Parameter field 'oops' must not be static";
  static final String PARAM_AND_VMPARAM =
      "Some fields have both @Param and @VmParam: [oops]";
  static final String RESERVED_PARAM =
      "Class '%s' uses reserved parameter name 'vm'";
  static final String NO_CONVERSION = "Type 'Object' of parameter field 'oops' "
      + "has no recognized String-converting method; see <TODO> for details";
  static final String CONVERT_FAILED = // granted this one's a little weird (and brittle)
      "Cannot convert value 'oops' to type 'int': For input string: \"oops\"";

  public void testDoesntExtendBenchmark() throws InvalidCommandException {
    String expected = String.format(DOESNT_EXTEND, Benchmark.class.getName());
    expectException(expected, NotABenchmark.class);
  }
  static class NotABenchmark {}

  public void testAbstract() throws InvalidCommandException {
    expectException(ABSTRACT, AbstractBenchmark.class);
  }
  abstract static class AbstractBenchmark extends Benchmark {}

  public void testNoSuitableConstructor() throws InvalidCommandException {
    expectException(NO_CONSTRUCTOR, BadConstructorBenchmark.class);
  }
  static class BadConstructorBenchmark extends Benchmark {
    BadConstructorBenchmark(String damnParam) {}
  }

  public void testNoBenchmarkMethods() throws InvalidCommandException {
    expectException(NO_METHODS, NoMethodsBenchmark.class);
  }
  static class NoMethodsBenchmark extends Benchmark {
    void timeIt(int reps) {} // not public
    public void timIt(int reps) {} // wrong name
  }

  public void testStaticBenchmarkMethod() throws InvalidCommandException {
    expectException(STATIC_BENCHMARK, StaticBenchmarkMethodBenchmark.class);
  }
  static class StaticBenchmarkMethodBenchmark extends Benchmark {
    public static void timeIt(int reps) {}
  }

  public void testWrongSignature() throws Exception {
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

  public void testStaticParam() throws InvalidCommandException {
    expectException(STATIC_PARAM, StaticParamBenchmark.class);
  }
  static class StaticParamBenchmark extends Benchmark {
    @Param static String oops;
  }

  public void testReservedParameterName() throws InvalidCommandException {
    expectException(RESERVED_PARAM, ReservedParamBenchmark.class);
  }
  static class ReservedParamBenchmark extends Benchmark {
    @Param String vm;
  }

  public void testUnparsableParamType() throws InvalidCommandException {
    expectException(NO_CONVERSION, UnparsableParamTypeBenchmark.class);
  }
  static class UnparsableParamTypeBenchmark extends Benchmark {
    @Param Object oops;
  }

  public void testUnparsableParamDefault() throws InvalidCommandException {
    expectException(CONVERT_FAILED, UnparsableParamDefaultBenchmark.class);
  }
  static class UnparsableParamDefaultBenchmark extends Benchmark {
    @Param({"1", "2", "oops"}) int number;
  }

  // end of tests

  private void expectException(String expectedMessageFmt, Class<?> benchmarkClass) {
    CaliperOptions options = new DefaultCaliperOptions(benchmarkClass.getName());
    try {
      // Note that all the failures checked by this test are caught before even calling run()
      ImmutableMap<String, String> map = ImmutableMap.of(
          "instrument.micro.class", MicrobenchmarkInstrument.class.getName());
      new ExperimentingCaliperRun(options, null, null, new BenchmarkClass(benchmarkClass),
          null, null, null, null, null, null, null).run();
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
