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
  private PrintWriter out = new PrintWriter(new StringWriter());
  private PrintWriter err = new PrintWriter(new StringWriter());

  @Test public void testExceptionInInit() throws Exception {
    try {
      CaliperMain.exitlessMain(new String[] {ExceptionInInitBenchmark.class.getName()}, out, err);
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
          new String[] {ExceptionInConstructorBenchmark.class.getName()}, out, err);
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
          new String[] {ExceptionInMethodBenchmark.class.getName()}, out, err);
      fail();
    } catch (UserCodeException expected) {}
  }

  static class ExceptionInMethodBenchmark extends Benchmark {
    public void timeSomething(int reps) {
      throw new RuntimeException();
    }
  }

  @Test public void testExceptionInMethod_notInDryRun() throws Exception {
    try {
      CaliperMain.exitlessMain(
          new String[] {ExceptionLateInMethodBenchmark.class.getName()}, out, err);
      fail();
    } catch (ProxyWorkerException expected) {
      expected.getMessage().contains(ExceptionLateInMethodBenchmark.class.getName());
    }
  }

  static class ExceptionLateInMethodBenchmark extends Benchmark {
    public void timeSomething(int reps) {
      if (reps > 1) {
        throw new RuntimeException();
      }
    }
  }

  @Test public void testExceptionInSetUp() throws Exception {
    try {
      CaliperMain.exitlessMain(
          new String[] {ExceptionInSetUpBenchmark.class.getName()}, out, err);
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
