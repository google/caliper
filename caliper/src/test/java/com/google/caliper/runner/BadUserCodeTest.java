package com.google.caliper.runner;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import com.google.caliper.config.InvalidConfigurationException;
import com.google.caliper.legacy.Benchmark;
import com.google.caliper.util.InvalidCommandException;
import com.google.common.collect.Lists;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.List;

/**
 * Integration tests for misbehaving benchmarks.
 */
@RunWith(JUnit4.class)
public class BadUserCodeTest {
  // TODO(gak): do something with this output so that test failures are useful
  private PrintWriter out = new PrintWriter(new StringWriter());
  private PrintWriter err = new PrintWriter(new StringWriter());

  @Test

  public void testExceptionInInit() throws Exception {
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

  @Test

  public void testExceptionInConstructor() throws Exception {
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

  @Test

  public void testExceptionInMethod() throws Exception {
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

  @Test

  public void testExceptionInMethod_notInDryRun() throws Exception {
    try {
      CaliperMain.exitlessMain(
          new String[] {ExceptionLateInMethodBenchmark.class.getName()}, out, err);
      fail();
    } catch (ProxyWorkerException expected) {
      assertTrue(expected.getMessage().contains(ExceptionLateInMethodBenchmark.class.getName()));
    }
  }

  static class ExceptionLateInMethodBenchmark extends Benchmark {
    public void timeSomething(int reps) {
      if (reps > 1) {
        throw new RuntimeException();
      }
    }
  }

  @Test

  public void testExceptionInSetUp() throws Exception {
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

  @Test

  public void testNonDeterministicAllocation_noTrackAllocations() throws Exception {
    try {
      runAllocationWorker(NonDeterministicAllocationBenchmark.class, false);
      fail();
    } catch (ProxyWorkerException expected) {
      String message = "Your benchmark appears to have non-deterministic allocation behavior";
      assertTrue("Expected " + expected.getMessage() + " to contain " + message,
          expected.getMessage().contains(message));
    }
  }

  @Test

  public void testNonDeterministicAllocation_trackAllocations() throws Exception {
    try {
      runAllocationWorker(NonDeterministicAllocationBenchmark.class, true);
      fail();
    } catch (ProxyWorkerException expected) {
      String message = "Your benchmark appears to have non-deterministic allocation behavior";
      assertTrue("Expected " + expected.getMessage() + " to contain " + message,
          expected.getMessage().contains(message));
    }
  }

  /** The number of allocations is non deterministic because it depends on static state. */
  static class NonDeterministicAllocationBenchmark extends Benchmark {
    static int timeCount = 0;
    // We dump items into this list so the jit cannot remove the allocations
    static List<Object> list = Lists.newArrayList();
    public int timeSomethingFBZ(int reps) {
      timeCount++;
      if (timeCount % 2 == 0) {
        list.add(new Object());
        return list.hashCode();
      }
      return this.hashCode();
    }
  }

  @Test

  public void testComplexNonDeterministicAllocation_noTrackAllocations() throws Exception {
    // Without trackAllocations enabled this kind of non-determinism cannot be detected.
    runAllocationWorker(ComplexNonDeterministicAllocationBenchmark.class, false);
  }

  @Test

  public void testComplexNonDeterministicAllocation_trackAllocations() throws Exception {
    try {
      runAllocationWorker(ComplexNonDeterministicAllocationBenchmark.class, true);
    } catch (ProxyWorkerException expected) {
      String message = "Your benchmark appears to have non-deterministic allocation behavior";
      assertTrue("Expected " + expected.getMessage() + " to contain " + message,
          expected.getMessage().contains(message));
    }
  }

  /** Benchmark allocates the same number of things each time but in a different way. */
  static class ComplexNonDeterministicAllocationBenchmark extends Benchmark {
    static int timeCount = 0;
    // We dump items into this list so the jit cannot remove the allocations
    static List<Object> list = Lists.newArrayList();
    public int timeSomethingFBZ(int reps) {
      timeCount++;
      if (timeCount % 2 == 0) {
        list.add(new Object());
      } else {
        list.add(new Object());
      }
      return list.hashCode();
    }
  }

  /**
   * Helper method to run the allocation worker against the given benchmark class.
   */
  private void runAllocationWorker(Class<?> benchmarkClass, boolean trackAllocations)
      throws InvalidCommandException, InvalidBenchmarkException, InvalidConfigurationException {
    CaliperMain.exitlessMain(
        new String[] {
            benchmarkClass.getName(),
            "-i",
            "allocation",
            "-Cinstrument.allocation.options.trackAllocations=" + trackAllocations,
            },
        out,
        err);
  }
}
