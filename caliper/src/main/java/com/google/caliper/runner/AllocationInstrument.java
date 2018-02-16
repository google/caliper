/*
 * Copyright (C) 2011 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.caliper.runner;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static java.util.logging.Level.SEVERE;

import com.google.caliper.Benchmark;
import com.google.caliper.api.Macrobenchmark;
import com.google.caliper.core.BenchmarkClassModel.MethodModel;
import com.google.caliper.core.InvalidBenchmarkException;
import com.google.caliper.model.InstrumentType;
import com.google.caliper.runner.config.VmConfig;
import com.google.caliper.runner.platform.Platform;
import com.google.caliper.runner.platform.SupportedPlatform;
import com.google.common.base.Optional;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import java.io.File;
import java.io.IOException;
import java.util.jar.JarFile;
import java.util.jar.Manifest;
import java.util.logging.Logger;

/**
 * {@link Instrument} that watches the memory allocations in an invocation of the benchmark method
 * and reports some statistic. The benchmark method must accept a single int argument 'reps', which
 * is the number of times to execute the guts of the benchmark method, and it must be public and
 * non-static.
 *
 * <p>Note that the allocation instruments reports a "worst case" for allocation in that it reports
 * the bytes and objects allocated in interpreted mode (no JIT).
 */
@SupportedPlatform(Platform.Type.JVM)
public final class AllocationInstrument extends Instrument {
  private static final String ALLOCATION_AGENT_JAR_OPTION = "allocationAgentJar";
  /**
   * If this option is set to {@code true} then every individual allocation will be tracked and
   * logged. This will also increase the detail of certain error messages.
   */
  private static final String TRACK_ALLOCATIONS_OPTION = "trackAllocations";

  /**
   * Valid names for the Premain-Class for the allocation instrumenter. This changed between 3.0 and
   * 3.1.0.
   */
  private static final ImmutableSet<String> ALLOCATION_INSTRUMENTER_PREMAIN_CLASS_NAMES =
      ImmutableSet.of(
          "com.google.monitoring.runtime.instrumentation.AllocationInstrumenter",
          "com.google.monitoring.runtime.instrumentation.AllocationInstrumenterBootstrap");

  private static final Logger logger = Logger.getLogger(AllocationInstrument.class.getName());

  @Override
  public boolean isBenchmarkMethod(MethodModel method) {
    return method.isAnnotationPresent(Benchmark.class)
        || BenchmarkMethods.isTimeMethod(method)
        || method.isAnnotationPresent(Macrobenchmark.class);
  }

  @Override
  public InstrumentedMethod createInstrumentedMethod(MethodModel benchmarkMethod)
      throws InvalidBenchmarkException {
    checkNotNull(benchmarkMethod);
    checkArgument(isBenchmarkMethod(benchmarkMethod));
    try {
      switch (BenchmarkMethods.Type.of(benchmarkMethod)) {
        case MACRO:
          return new MacroAllocationInstrumentedMethod(benchmarkMethod);
        case MICRO:
        case PICO:
          return new MicroAllocationInstrumentedMethod(benchmarkMethod);
        default:
          throw new AssertionError("unknown type");
      }
    } catch (IllegalArgumentException e) {
      throw new InvalidBenchmarkException(
          "Benchmark methods must have no arguments or accept "
              + "a single int or long parameter: %s",
          benchmarkMethod.name());
    }
  }

  private final class MicroAllocationInstrumentedMethod extends InstrumentedMethod {
    MicroAllocationInstrumentedMethod(MethodModel benchmarkMethod) {
      super(benchmarkMethod);
    }

    @Override
    public ImmutableMap<String, String> workerOptions() {
      return ImmutableMap.of(TRACK_ALLOCATIONS_OPTION, options.get(TRACK_ALLOCATIONS_OPTION));
    }

    @Override
    public InstrumentType type() {
      return InstrumentType.ALLOCATION_MICRO;
    }

    @Override
    MeasurementCollectingVisitor getMeasurementCollectingVisitor() {
      return new Instrument.DefaultMeasurementCollectingVisitor(
          ImmutableSet.of("bytes", "objects"));
    }
  }

  @Override
  public TrialSchedulingPolicy schedulingPolicy() {
    // Assuming there is enough memory it should be fine to run these in parallel.
    return TrialSchedulingPolicy.PARALLEL;
  }

  private final class MacroAllocationInstrumentedMethod extends InstrumentedMethod {
    MacroAllocationInstrumentedMethod(MethodModel benchmarkMethod) {
      super(benchmarkMethod);
    }

    @Override
    public ImmutableMap<String, String> workerOptions() {
      return ImmutableMap.of(TRACK_ALLOCATIONS_OPTION, options.get(TRACK_ALLOCATIONS_OPTION));
    }

    @Override
    public InstrumentType type() {
      return InstrumentType.ALLOCATION_MACRO;
    }

    @Override
    MeasurementCollectingVisitor getMeasurementCollectingVisitor() {
      return new Instrument.DefaultMeasurementCollectingVisitor(
          ImmutableSet.of("bytes", "objects"));
    }
  }

  @Override
  public ImmutableSet<String> instrumentOptions() {
    return ImmutableSet.of(ALLOCATION_AGENT_JAR_OPTION, TRACK_ALLOCATIONS_OPTION);
  }

  private static Optional<File> findAllocationInstrumentJarOnClasspath() throws IOException {
    ImmutableSet<File> jarFiles =
        JarFinder.findJarFiles(
            Thread.currentThread().getContextClassLoader(), ClassLoader.getSystemClassLoader());
    for (File file : jarFiles) {
      JarFile jarFile = null;
      try {
        jarFile = new JarFile(file);
        Manifest manifest = jarFile.getManifest();
        if ((manifest != null)
            && ALLOCATION_INSTRUMENTER_PREMAIN_CLASS_NAMES.contains(
                manifest.getMainAttributes().getValue("Premain-Class"))) {
          return Optional.of(file);
        }
      } finally {
        if (jarFile != null) {
          jarFile.close();
        }
      }
    }
    return Optional.absent();
  }

  /**
   * This instrument's worker requires the allocationinstrumenter agent jar, specified on the worker
   * VM's command line with "-javaagent:[jarfile]".
   */
  @Override
  ImmutableSet<String> getExtraCommandLineArgs(VmConfig vmConfig) {
    String agentJar = options.get(ALLOCATION_AGENT_JAR_OPTION);
    if (Strings.isNullOrEmpty(agentJar)) {
      try {
        Optional<File> instrumentJar = findAllocationInstrumentJarOnClasspath();
        // TODO(gak): bundle up the allocation jar and unpack it if it's not on the classpath
        if (instrumentJar.isPresent()) {
          agentJar = instrumentJar.get().getAbsolutePath();
        }
      } catch (IOException e) {
        logger.log(
            SEVERE,
            "An exception occurred trying to locate the allocation agent jar on the classpath",
            e);
      }
    }
    if (Strings.isNullOrEmpty(agentJar) || !new File(agentJar).exists()) {
      throw new IllegalStateException("Can't find required allocationinstrumenter agent jar");
    }
    // Add microbenchmark args to minimize differences in the output
    return new ImmutableSet.Builder<String>()
        // we just run in interpreted mode to ensure that intrinsics don't break the instrumentation
        .add("-Xint")
        .add("-javaagent:" + agentJar)
        // Some environments rename files and use symlinks to improve resource caching,
        // if the agent jar path is actually a symlink it will prevent the agent from finding itself
        // and adding itself to the bootclasspath, so we do it manually here.
        .add("-Xbootclasspath/a:" + agentJar)
        .build();
  }
}
