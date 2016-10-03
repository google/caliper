package com.google.caliper.runner;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.google.caliper.Benchmark;
import com.google.caliper.api.Macrobenchmark;
import java.util.Collections;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/** Tests {@link BenchmarkClassChecker}. */

@RunWith(JUnit4.class)
public class BenchmarkClassCheckerTest {

  private BenchmarkClassChecker benchmarkClassChecker =
      BenchmarkClassChecker.create(Collections.<String>emptyList());

  @Test
  public void testNoBenchmarkMethods() {
    assertFalse(benchmarkClassChecker.isBenchmark(Object.class));
  }

  @Test
  public void testBenchmarkAnnotatedMethod() {
    assertTrue(benchmarkClassChecker.isBenchmark(BenchmarkAnnotatedMethod.class));
  }

  public static class BenchmarkAnnotatedMethod {
    @Benchmark
    void benchmarkMethod() {}
  }

  @Test
  public void testMacroBenchmarkAnnotatedMethod() {
    assertTrue(benchmarkClassChecker.isBenchmark(MacroBenchmarkAnnotatedMethod.class));
  }

  public static class MacroBenchmarkAnnotatedMethod {
    @Macrobenchmark
    void macrobenchmarkMethod() {}
  }

  @Test
  public void testTimeMethod() {
    assertTrue(benchmarkClassChecker.isBenchmark(TimeMethod.class));
  }

  public static class TimeMethod {
    public void timeMethod() {}
  }
}
