package com.google.caliper.runner;

import static org.junit.Assert.fail;

import com.google.caliper.Benchmark;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.io.PrintWriter;
import java.io.StringWriter;

/**
 * Integration tests for misbehaving benchmarks.
 */
@RunWith(JUnit4.class)

public class BadUserCodeTest {
  private PrintWriter writer = new PrintWriter(new StringWriter());

  @Test public void testExceptionInInit() throws Exception {
    try {
      CaliperMain.exitlessMain(new String[] {ExceptionInInitBenchmark.class.getName()}, writer);
      fail();
    } catch (UserCodeException expected) {}
  }

  private static void throwSomeUserException() {
    throw new RuntimeException();
  }

  static class ExceptionInInitBenchmark extends Benchmark {
    static {
      throwSomeUserException();
    }

    public void timeSomething(int reps) {
      fail("" + reps);
    }
  }

  @Test public void testExceptionInConstructor() throws Exception {
    try {
      CaliperMain.exitlessMain(
          new String[] {ExceptionInConstructorBenchmark.class.getName()}, writer);
      fail();
    } catch (UserCodeException expected) {}
  }

  static class ExceptionInConstructorBenchmark extends Benchmark {
    ExceptionInConstructorBenchmark() {
      throw new RuntimeException();
    }

    public void timeSomething(int reps) {
      fail("" + reps);
    }
  }

  @Test public void testExceptionInMethod() throws Exception {
    try {
      CaliperMain.exitlessMain(
          new String[] {ExceptionInConstructorBenchmark.class.getName()}, writer);
      fail();
    } catch (UserCodeException expected) {}
  }

  static class ExceptionInMethodBenchmark extends Benchmark {
    public void timeSomething(int reps) {
      throw new RuntimeException();
    }
  }

  @Test public void testExceptionInSetUp() throws Exception {
    try {
      CaliperMain.exitlessMain(
          new String[] {ExceptionInSetUpBenchmark.class.getName()}, writer);
      fail();
    } catch (UserCodeException expected) {}
  }

  static class ExceptionInSetUpBenchmark extends Benchmark {
    @Override protected void setUp() {
      throw new RuntimeException();
    }

    public void timeSomething(int reps) {
      fail("" + reps);
    }
  }
}
