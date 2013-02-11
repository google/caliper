package com.google.caliper.runner;

import static org.junit.Assert.assertEquals;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;

import org.junit.Test;

import java.io.File;

/**
 * Tests {@link AllocationInstrument}.
 */

public class AllocationInstrumentTest {
  @Test public void testGetExtraCommandLineArgs() throws Exception {
    AllocationInstrument instrument = new AllocationInstrument();
    File fakeJar = File.createTempFile("fake", "jar");
    fakeJar.deleteOnExit();
    instrument.setOptions(ImmutableMap.of("allocationAgentJar", fakeJar.getAbsolutePath()));
    ImmutableSet<String> expected = new ImmutableSet.Builder<String>()
        .addAll(Instrument.JVM_ARGS)
        .add("-javaagent:" + fakeJar.getAbsolutePath())
        .add("-Dsun.reflect.inflationThreshold=0")
        .build();
    assertEquals(expected, instrument.getExtraCommandLineArgs());
    fakeJar.delete();
  }
}
