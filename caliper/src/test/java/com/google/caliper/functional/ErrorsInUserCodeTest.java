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
import com.google.caliper.Runner;
import com.google.caliper.UserException.AbstractBenchmarkException;
import com.google.caliper.UserException.DoesntImplementBenchmarkException;
import com.google.caliper.UserException.ExceptionFromUserCodeException;
import com.google.caliper.UserException.NoParameterlessConstructorException;
import junit.framework.TestCase;

/**
 * Unit test covering common user mistakes.
 */
public class ErrorsInUserCodeTest extends TestCase {
  private Runner runner;

  @Override protected void setUp() throws Exception {
    runner = new Runner();
  }

  public void testDidntSubclassAnything() {
    try {
      runner.run(NotABenchmark.class.getName());
      fail();
    } catch (ExceptionFromUserCodeException expected) {
      assertEquals(DoesntImplementBenchmarkException.class.getCanonicalName(),
    		  expected.getCause().getClass().getCanonicalName());
    }
  }

  static class NotABenchmark {
    public void timeSomething(int reps) {
      fail("" + reps);
    }
  }


  public void testAbstract() {
    try {
      runner.run(AbstractBenchmark.class.getName());
      fail();
    } catch (ExceptionFromUserCodeException expected) {
        assertEquals(AbstractBenchmarkException.class.getCanonicalName(),
      		  expected.getCause().getClass().getCanonicalName());
    }
  }

  abstract static class AbstractBenchmark extends Benchmark {
    public void timeSomething(int reps) {
      fail("" + reps);
    }
  }


  public void testNoSuitableConstructor() {
    try {
      runner.run(BadConstructorBenchmark.class.getName());
      fail();
    } catch (ExceptionFromUserCodeException expected) {
        assertEquals(NoParameterlessConstructorException.class.getCanonicalName(),
        		  expected.getCause().getClass().getCanonicalName());
    }
  }

  static class BadConstructorBenchmark extends Benchmark {
    BadConstructorBenchmark(String damnParam) {
      fail(damnParam);
    }

    public void timeSomething(int reps) {
      fail("" + reps);
    }
  }


  @SuppressWarnings("serial")
  static class SomeUserException extends RuntimeException {}

  private static void throwSomeUserException() {
    throw new SomeUserException();
  }

  public void testExceptionInInit() {
    try {
      runner.run(ExceptionInInitBenchmark.class.getName());
      fail();
    } catch (ExceptionFromUserCodeException expected) {
    }
  }

  static class ExceptionInInitBenchmark extends Benchmark {
    static {
      throwSomeUserException();
    }

    public void timeSomething(int reps) {
      fail("" + reps);
    }
  }

  public void testExceptionInConstructor() {
    try {
      runner.run(ExceptionInConstructorBenchmark.class.getName());
      fail();
    } catch (ExceptionFromUserCodeException expected) {
    }
  }

  static class ExceptionInConstructorBenchmark extends Benchmark {
    ExceptionInConstructorBenchmark() {
      throw new SomeUserException();
    }

    public void timeSomething(int reps) {
      fail("" + reps);
    }
  }

  public void testExceptionInMethod() {
    try {
      new Runner().run(ExceptionInMethodBenchmark.class.getName());
      fail();
    } catch (ExceptionFromUserCodeException ignored) {
    }
  }

  public static class ExceptionInMethodBenchmark extends Benchmark {
    public void timeSomething(int reps) {
      throw new SomeUserException();
    }
  }

  public void testUserCodePrintsOutput() {
    new Runner().run(UserCodePrintsBenchmark.class.getName(), "--debug",
        "--warmupMillis", "100", "--runMillis", "100");
  }

  public static class UserCodePrintsBenchmark extends Benchmark {
    public void timeSomething(int reps) throws InterruptedException {
      System.out.println("output to System.out!");
      Thread.sleep(reps);
    }
  }
}
