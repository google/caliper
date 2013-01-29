package examples;

import com.google.caliper.Benchmark;
import com.google.caliper.runner.CaliperMain;

/**
 * This is the absolute minimal benchmark. It does nothing but time the rep loop.
 */
public class NoOpBenchmark extends Benchmark {
  public long timeIncrement(long reps) {
    long result = 0;
    for (; result < reps; result++) {}
    return result;
  }

  public static void main(String[] args) throws Exception {
    CaliperMain.main(NoOpBenchmark.class, args);
  }
}
