package com.google.caliper.runner;

import static com.google.common.base.Preconditions.checkState;

import com.google.caliper.config.InvalidConfigurationException;
import com.google.caliper.model.Trial;
import com.google.caliper.util.InvalidCommandException;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;

import org.junit.rules.TestWatcher;
import org.junit.runner.Description;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Arrays;
import java.util.List;

/**
 * A {@link TestWatcher} that can be used to configure a run of caliper.
 * 
 * <p>Provides common test configuration utilities and redirects output to a buffer and only dumps
 * it during a failure.
 * 
 * <p>TODO(lukes,gak): This is a bad name since it isn't just watching the tests, it is helping you
 * run the tests.
 */
public final class CaliperTestWatcher extends TestWatcher {
  // N.B. StringWriter is internally synchronized and is safe to write to from multiple threads.
  private StringWriter output;

  private boolean verbose = true;
  private String instrument;
  private Class<?> benchmarkClass;
  private List<String> extraOptions = Lists.newArrayList();
  
  CaliperTestWatcher forBenchmark(Class<?> benchmarkClass) {
    this.benchmarkClass = benchmarkClass;
    return this;
  }
  
  CaliperTestWatcher instrument(String instrument) {
    this.instrument = instrument;
    return this;
  }
  
  CaliperTestWatcher verbose(boolean verbose) {
    this.verbose = verbose;
    return this;
  }
  
  CaliperTestWatcher options(String... extraOptions) {
    this.extraOptions = Arrays.asList(extraOptions);
    return this;
  }
  
  void run() throws InvalidCommandException, InvalidBenchmarkException, 
      InvalidConfigurationException {
    checkState(benchmarkClass != null, "You must configure a benchmark!");
    List<String> options = Lists.newArrayList(
        "-Cresults.file.class=",
        "-Cresults.upload.class=" + InMemoryResultsUploader.class.getName());
    if (instrument != null) {
      options.add("-i");
      options.add(instrument);
    }
    if (verbose) {
      options.add("-v");
    }
    options.addAll(extraOptions);
    options.add(benchmarkClass.getName());
    this.output = new StringWriter();
    CaliperMain.exitlessMain(options.toArray(new String[0]), new PrintWriter(output,  true), 
        new PrintWriter(output,  true)); 
  }
  
  @Override protected void failed(Throwable e, Description description) {
    // don't log if run was never called.
    if (output != null) {
      System.err.println("Caliper failed with the following output:\n" + output.toString());
    }
  }
  
  ImmutableList<Trial> trials() {
    return InMemoryResultsUploader.trials();
  }
}
