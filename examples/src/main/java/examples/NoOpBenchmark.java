package examples;

import com.google.caliper.legacy.Benchmark;


/**
 * This is the absolute minimal benchmark. It does nothing but time the rep loop.
 */
public class NoOpBenchmark extends Benchmark {
  public long timeIncrement(long reps) {
    long result = 0;
    for (; result < reps; result++) {}
    return result;
  }
}
