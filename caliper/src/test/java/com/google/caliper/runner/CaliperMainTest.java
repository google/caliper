package com.google.caliper.runner;

import static com.google.common.truth.Truth.assertThat;

import java.io.PrintWriter;
import java.io.StringWriter;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/** Integration tests for the main method. */
@RunWith(JUnit4.class)
public final class CaliperMainTest {

  // Regression test for '-h', at one point it broke due to the order in which things were injected
  @Test
  public void testHelp() throws Exception {
    StringWriter out = new StringWriter();
    StringWriter err = new StringWriter();
    int exitCode =
        CaliperMain.createRunner(
                new String[] {"-h"}, new PrintWriter(out, true), new PrintWriter(err, true))
            .run();
    assertThat(exitCode).isEqualTo(0);
    assertThat(out.toString())
        .startsWith("Usage:\n java com.google.caliper.runner.CaliperMain <benchmark_class_name>");
    assertThat(err.toString()).isEmpty();
  }
}
