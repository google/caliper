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
import com.google.caliper.config.CaliperRc;
import com.google.caliper.runner.CaliperOptions;
import com.google.caliper.runner.CaliperRun;
import com.google.caliper.runner.ConsoleWriter;
import com.google.caliper.runner.InvalidBenchmarkException;
import com.google.caliper.runner.MicrobenchmarkInstrument;
import com.google.caliper.runner.SilentConsoleWriter;
import com.google.caliper.runner.UserCodeException;
import com.google.caliper.util.InvalidCommandException;
import com.google.common.collect.ImmutableMap;

import junit.framework.TestCase;

/**
 * Unit test covering common user mistakes in benchmark classes.
 */
public class ExceptionsFromUserCodeTest extends TestCase {
  public void testExceptionInInit() throws Exception {
    expectException(ExceptionInInitBenchmark.class);
  }
  static class ExceptionInInitBenchmark extends Benchmark {
    static {
      throwSomeUserException();
    }
  }

  public void testExceptionInConstructor() throws Exception {
    expectException(ExceptionInConstructorBenchmark.class);
  }
  static class ExceptionInConstructorBenchmark extends Benchmark {
    private ExceptionInConstructorBenchmark() {
      throwSomeUserException();
    }
    public void timeSomething(int reps) {}
  }

  public void testExceptionInSetUp() throws Exception {
    expectException(ExceptionInSetUpBenchmark.class);
  }
  static class ExceptionInSetUpBenchmark extends Benchmark {
    @Override protected void setUp() {
      throwSomeUserException();
    }
    public void timeSomething(int reps) {}
  }

//  public void testExceptionInTimeMethod() throws Exception {
//    expectException(ExceptionInTimeMethodBenchmark.class);
//  }
//  static class ExceptionInTimeMethodBenchmark extends Benchmark {
//    public void timeSomething(int reps) {
//      throwSomeUserException();
//    }
//  }

  // end of tests

  private void expectException(Class<?> benchmarkClass)
      throws InvalidCommandException, InvalidBenchmarkException {
    CaliperOptions options = new DefaultCaliperOptions(benchmarkClass.getName());
    try {
      // It's undefined whether these exceptions happen during the constructor or run()
      ImmutableMap<String, String> map = ImmutableMap.of(
          "instrument.micro.class", MicrobenchmarkInstrument.class.getName());
      new CaliperRun(options, new CaliperRc(map).asCaliperConfig(), SHH).run();
      fail("no exception thrown");
    } catch (UserCodeException e) {
      if (!(e.getCause() instanceof SomeUserException)) {
        throw e;
      }
    }
  }

  private static final ConsoleWriter SHH = new SilentConsoleWriter();

  @SuppressWarnings("serial")
  static class SomeUserException extends RuntimeException {}

  private static void throwSomeUserException() {
    throw new SomeUserException();
  }
}

