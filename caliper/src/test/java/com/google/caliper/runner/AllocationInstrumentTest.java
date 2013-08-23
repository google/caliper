package com.google.caliper.runner;

import static org.junit.Assert.assertEquals;

import com.google.caliper.Benchmark;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.io.File;
import java.util.List;

/**
 * Tests {@link AllocationInstrument}.
 */

@RunWith(JUnit4.class)
public class AllocationInstrumentTest {

  @Rule public CaliperTestWatcher runner = new CaliperTestWatcher();

  @Test public void testGetExtraCommandLineArgs() throws Exception {
    AllocationInstrument instrument = new AllocationInstrument();
    File fakeJar = File.createTempFile("fake", "jar");
    fakeJar.deleteOnExit();
    instrument.setOptions(ImmutableMap.of("allocationAgentJar", fakeJar.getAbsolutePath()));
    ImmutableSet<String> expected = new ImmutableSet.Builder<String>()
        .addAll(Instrument.JVM_ARGS)
        .add("-javaagent:" + fakeJar.getAbsolutePath())
        .add("-Xbootclasspath/a:" + fakeJar.getAbsolutePath())
        .add("-Dsun.reflect.inflationThreshold=0")
        .build();
    assertEquals(expected, instrument.getExtraCommandLineArgs());
    fakeJar.delete();
  }

  public static class TestBenchmark {
    List<Object> list = Lists.newLinkedList();
    @Benchmark public int compressionSize(int reps) {
      for (int i = 0; i < reps; i++) {
        list.add(new Object());
      }
      int hashCode = list.hashCode();
      list.clear();
      return hashCode;
    }
  }
}
