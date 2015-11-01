package dk.ilios.spanner;

/**
 * The expected granularity of the benchmark. This is used to optimize how a benchmark is executed as well as timers
 * used.
 */
public enum BenchmarkGranularity {
    MILLIS, // Benchmarks measured in milliseconds
    NANOS, // Benchmarks measured in nanonseconds.
    PICOS // Benchmarks measured in pico seconds.
}
