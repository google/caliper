package com.google.caliper.worker;

import static com.google.caliper.worker.RuntimeWorker.INITIAL_REPS;
import static com.google.caliper.worker.RuntimeWorker.calculateTargetReps;
import static java.util.concurrent.TimeUnit.HOURS;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static java.util.concurrent.TimeUnit.NANOSECONDS;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.junit.Assert.assertEquals;

import com.google.caliper.util.ShortDuration;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.math.BigDecimal;

/**
 * Tests {@link RuntimeWorker}.
 */
@RunWith(JUnit4.class)
public class RuntimeWorkerTest {
  private static final ShortDuration TIMING_INTERVAL = ShortDuration.of(100, MILLISECONDS);

  @Test public void testCalculateTargetReps_tinyBenchmark() {
    // this is one cycle on a 5GHz machine
    ShortDuration oneCycle = ShortDuration.of(new BigDecimal("2.0e-10"), SECONDS);
    long targetReps = calculateTargetReps(INITIAL_REPS,
        oneCycle.times(INITIAL_REPS).to(NANOSECONDS), TIMING_INTERVAL.to(NANOSECONDS), 0.0);
    long expectedReps = TIMING_INTERVAL.toPicos() / oneCycle.toPicos();
    assertEquals(expectedReps, targetReps);
  }

  @Test public void testCalculateTargetReps_hugeBenchmark() {
    long targetReps =
        calculateTargetReps(INITIAL_REPS, HOURS.toNanos(1), TIMING_INTERVAL.to(NANOSECONDS), 0.0);
    assertEquals(1, targetReps);
  }

  @Test public void testCalculateTargetReps_applyRandomness() {
    long targetReps = calculateTargetReps(INITIAL_REPS, MILLISECONDS.toNanos(100),
        TIMING_INTERVAL.to(NANOSECONDS), 0.5);
    assertEquals(110, targetReps);
  }
}
