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

import com.google.caliper.api.Benchmark;
import com.google.caliper.api.Param;
import com.google.caliper.api.VmParam;
import com.google.caliper.runner.CaliperOptions;
import com.google.caliper.runner.CaliperRc;
import com.google.caliper.runner.CaliperRun;
import com.google.caliper.runner.InvalidBenchmarkException;
import com.google.caliper.runner.UserCodeException;
import com.google.caliper.util.InvalidCommandException;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;

import junit.framework.AssertionFailedError;
import junit.framework.TestCase;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;

/**
 * Unit test covering common user mistakes in benchmark classes.
 */
public class InvalidBenchmarksTest extends TestCase {
  private final CaliperRc caliperRc = new CaliperRc(ImmutableMap.<String, String>of());

  // Put the expected messages together here, which may promote some kind of
  // consistency in their wording. :)

  static final String DOESNT_EXTEND =
      "Class '%%s' does not extend %s";
  static final String ABSTRACT =
      "Class '%s' is abstract";
  static final String NO_CONSTRUCTOR =
      "Class '%s' has no parameterless constructor";
  static final String EXCEPTION_IN_INIT =
      "Exception thrown while initializing class '%s'";
  static final String EXCEPTION_IN_CONSTR =
      "Exception thrown from benchmark constructor";
  static final String PARAM_AND_VMPARAM =
      "Some fields have both @Param and @VmParam: [oops]";
  static final String NO_METHODS =
      "Class '%s' contains no benchmark methods for instrument 'microbenchmark'";
  static final String WRONG_ARGUMENTS =
      "Microbenchmark methods must accept a single int parameter: %s";
  static final String RESERVED_PARAM =
      "Class '%s' uses reserved parameter name 'vm'";
  static final String NO_CONVERSION = "Type 'Object' of parameter field 'oops' "
      + "has no static 'fromString(String)' or 'valueOf(String)' method";
  static final String CONVERT_FAILED =
      "Cannot convert default value 'oops' to type 'Integer': Wrong argument format: oops";
  static final String DEFAULTS_FIELD_NOT_STATIC =
      "Default-values field 'fooValues' is not static";
  static final String DEFAULTS_METHOD_NOT_STATIC =
      "Default-values method 'fooValues' is not static";
  static final String DEFAULTS_WRONG_TYPE =
      "Default values must be of type Iterable<Long> (or any subtype)";
  static final String DEFAULTS_FIELD_EMPTY =
      "Default-values field 'fooValues' has no values";
  static final String DEFAULTS_METHOD_EMPTY =
      "Default-values method 'fooValues' returned no values";
  static final String USER_EXCEPTION =
      "An exception was thrown from the benchmark code";
  static final String VMPARAM_NOT_STRING =
      "Parameter field 'uhoh' is marked with @VmParam but is not of type String";


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

  public void testExceptionInInit() throws InvalidCommandException {
    String message = String.format(EXCEPTION_IN_INIT, ExceptionInInitBenchmark.class.getName());
    expectUserException(message, ExceptionInInitBenchmark.class);
  }
  static class ExceptionInInitBenchmark extends Benchmark {
    static {
      throwSomeUserException();
    }
  }

  public void testExceptionInConstructor() throws InvalidCommandException {
    expectUserException(EXCEPTION_IN_CONSTR, ExceptionInConstructorBenchmark.class);
  }
  static class ExceptionInConstructorBenchmark extends Benchmark {
    private ExceptionInConstructorBenchmark() {
      throwSomeUserException();
    }
  }

  public void testFieldIsBothParamAndVmParam() throws InvalidCommandException {
    expectException(PARAM_AND_VMPARAM, ParamAndVmParamBenchmark.class);
  }
  static class ParamAndVmParamBenchmark extends Benchmark {
    @Param @VmParam String oops;
  }

  public void testNoBenchmarkMethods() throws InvalidCommandException {
    expectException(NO_METHODS, NoMethodsBenchmark.class);
  }
  static class NoMethodsBenchmark extends Benchmark {
    void timeIt(int reps) {} // not public
    public void timIt(int reps) {} // wrong name
  }

  public void testWrongSignature() throws Exception {
    testWrongSignature(WrongSignatureBenchmark1.class);
    testWrongSignature(WrongSignatureBenchmark2.class, Integer.class);
    testWrongSignature(WrongSignatureBenchmark3.class, int.class, int.class);
  }
  private void testWrongSignature(Class<?> c, Class<?>... args) throws Exception {
    Method method = c.getDeclaredMethod("timeIt", args);
    expectException(String.format(WRONG_ARGUMENTS, method), c);
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

  public void testDefaultsFieldNotStatic() throws InvalidCommandException {
    expectException(DEFAULTS_FIELD_NOT_STATIC, DefaultsFieldNotStaticBenchmark.class);
  }
  static class DefaultsFieldNotStaticBenchmark extends Benchmark {
    @Param String foo;
    public List<String> fooValues;
  }

  public void testDefaultsMethodNotStatic() throws InvalidCommandException {
    expectException(DEFAULTS_METHOD_NOT_STATIC, DefaultsMethodNotStaticBenchmark.class);
  }
  static class DefaultsMethodNotStaticBenchmark extends Benchmark {
    @Param String foo;

    public List<String> fooValues() {
      return null;
    }
  }

  public void testDefaultsFieldIsntIterable() throws InvalidCommandException {
    expectException(DEFAULTS_WRONG_TYPE, DefaultsFieldIsntIterableBenchmark.class);
  }
  static class DefaultsFieldIsntIterableBenchmark extends Benchmark {
    @Param Long foo;
    public static String fooValues = "oops";
  }

  public void testDefaultsMethodIsntIterable() throws InvalidCommandException {
    expectException(DEFAULTS_WRONG_TYPE, DefaultsMethodIsntIterableBenchmark.class);
  }
  static class DefaultsMethodIsntIterableBenchmark extends Benchmark {
    @Param Long foo;

    public static String fooValues() {
      return "oops";
    }
  }

  public void testDefaultsFieldContainsAWrongType() throws InvalidCommandException {
    expectException(DEFAULTS_WRONG_TYPE, DefaultsFieldContainsAWrongTypeBenchmark.class);
  }
  static class DefaultsFieldContainsAWrongTypeBenchmark extends Benchmark {
    @Param Long foo;
    public static Iterable<Object> fooValues = Arrays.<Object>asList(1L, 2L, "oops");
  }

  public void testDefaultsMethodContainsAWrongType() throws InvalidCommandException {
    expectException(DEFAULTS_WRONG_TYPE, DefaultsMethodContainsAWrongTypeBenchmark.class);
  }
  static class DefaultsMethodContainsAWrongTypeBenchmark extends Benchmark {
    @Param Long foo;

    public static Iterable<Object> fooValues() {
      return Arrays.<Object>asList(1L, 2L, "oops");
    }
  }

  public void testDefaultsFieldIsEmpty() throws InvalidCommandException {
    expectException(DEFAULTS_FIELD_EMPTY, DefaultsFieldIsEmptyBenchmark.class);
  }
  static class DefaultsFieldIsEmptyBenchmark extends Benchmark {
    @Param Long foo;
    public static List<Long> fooValues = ImmutableList.of();
  }

  public void testDefaultsMethodIsEmpty() throws InvalidCommandException {
    expectException(DEFAULTS_METHOD_EMPTY, DefaultsMethodIsEmptyBenchmark.class);
  }
  static class DefaultsMethodIsEmptyBenchmark extends Benchmark {
    @Param Long foo;

    public static List<Long> fooValues() {
      return ImmutableList.of();
    }
  }

  public void testDefaultsMethodThrows() throws InvalidCommandException {
    expectUserException(USER_EXCEPTION, DefaultsMethodThrowsBenchmark.class);
  }
  static class DefaultsMethodThrowsBenchmark extends Benchmark {
    @Param Long foo;

    public static Iterable<Long> fooValues() {
      throw new SomeUserException();
    }
  }

  public void testVmParamIsNotString() throws InvalidCommandException {
    expectException(VMPARAM_NOT_STRING, VmParamIsNotStringBenchmark.class);
  }
  static class VmParamIsNotStringBenchmark extends Benchmark {
    @VmParam int uhoh;
  }

  // end of tests

  private void expectException(String expectedMessageFmt, Class<?> benchmarkClass)
      throws InvalidCommandException {
    expectException(expectedMessageFmt, benchmarkClass, InvalidBenchmarkException.class);
  }

  private void expectUserException(String expectedMessageFmt, Class<?> benchmarkClass)
      throws InvalidCommandException {
    expectException(expectedMessageFmt, benchmarkClass, UserCodeException.class);
    // TODO(kevinb): ideally we would check that the user's exception was chained...
  }

  private void expectException(
      String expectedMessageFmt,
      Class<?> benchmarkClass,
      Class<? extends InvalidBenchmarkException> expectedType)
      throws InvalidCommandException {
    CaliperOptions options = new DefaultCaliperOptions(benchmarkClass.getName());
    try {
      new CaliperRun(options, caliperRc, null);
      fail("no exception thrown");

      // Note that all the failures checked by this test are caught before even calling run()

    } catch (InvalidBenchmarkException e) {
      try {
        Class<?> actualType = e.getClass();
        assertTrue("wrong exception type: " + actualType,
            expectedType.isAssignableFrom(actualType));
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

  @SuppressWarnings("serial")
  static class SomeUserException extends RuntimeException {}

  private static void throwSomeUserException() {
    throw new SomeUserException();
  }
}
