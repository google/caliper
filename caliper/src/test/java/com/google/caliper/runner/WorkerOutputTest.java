package com.google.caliper.runner;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.google.caliper.Benchmark;
import com.google.caliper.worker.Worker;
import com.google.common.collect.ObjectArrays;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.io.PrintWriter;
import java.io.StringWriter;

/**
 * Test that the output from the {@link Worker} is routed correctly.
 */
@RunWith(JUnit4.class)

public class WorkerOutputTest {
  private static final String[] TEST_CONFIGURATION_OPTIONS = new String[] {
    "-Cresults.file.class=",
    "-Cresults.upload.class=",
    "-Cinstrument.micro.options.warmup=1ms",
    "-Cinstrument.micro.options.timingInterval=1ms",
    "-Cinstrument.micro.options.measurements=1",
    "--instrument=micro"
  };

  @Test public void workerOutputDoesNotGetPrinted() throws Exception {
    StringWriter stringWriter = new StringWriter();
    CaliperMain.exitlessMain(
        ObjectArrays.concat(TEST_CONFIGURATION_OPTIONS, SystemOutAndErrBenchmark.class.getName()),
        new PrintWriter(stringWriter));
    assertFalse(stringWriter.toString().contains("hello, out"));
    assertFalse(stringWriter.toString().contains("hello, err"));
  }

  @Test public void verboseWorkerOutputDoesGetPrinted() throws Exception {
    StringWriter stringWriter = new StringWriter();
    CaliperMain.exitlessMain(
        ObjectArrays.concat(TEST_CONFIGURATION_OPTIONS,
            new String[] {"--verbose", SystemOutAndErrBenchmark.class.getName()}, String.class),
        new PrintWriter(stringWriter));
    assertTrue(stringWriter.toString().contains("hello, out"));
    assertTrue(stringWriter.toString().contains("hello, err"));
  }

  public static class SystemOutAndErrBenchmark extends Benchmark {
    public void timeSystemOutAndSystemErr(int reps) {
      for (int i = 0; i < reps; i++) {
        System.out.println("hello, out");
        System.err.println("hello, err");
      }
    }
  }
}
