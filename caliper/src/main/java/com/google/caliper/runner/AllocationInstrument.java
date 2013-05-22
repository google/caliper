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

import static com.google.common.base.Throwables.propagateIfInstanceOf;
import static java.util.logging.Level.SEVERE;

import com.google.caliper.api.SkipThisScenarioException;
import com.google.caliper.worker.AllocationWorker;
import com.google.caliper.worker.Worker;
import com.google.common.base.Optional;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableSet;
import com.google.monitoring.runtime.instrumentation.AllocationInstrumenter;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.jar.JarFile;
import java.util.jar.Manifest;
import java.util.logging.Logger;

/**
 * {@link Instrument} that watches the memory allocations in an invocation of the
 * benchmark method and reports some statistic. The benchmark method must accept a
 * single int argument 'reps', which is the number of times to execute the guts of
 * the benchmark method, and it must be public and non-static.
 */
public final class AllocationInstrument extends Instrument {
  private static final String ALLOCATION_AGENT_JAR_OPTION = "allocationAgentJar";
  private static final Logger logger = Logger.getLogger(AllocationInstrumenter.class.getName());

  @Override public boolean isBenchmarkMethod(Method method) {
    return Instrument.isTimeMethod(method);
  }

  @Override public Method checkBenchmarkMethod(BenchmarkClass benchmarkClass, Method method)
      throws InvalidBenchmarkException {
    return Instrument.checkTimeMethod(benchmarkClass, method);
  }

  @Override
  public void dryRun(Object benchmark, Method benchmarkMethod) throws UserCodeException {
    // execute the benchmark method, but don't try to take any measurements, because this JVM
    // may not have the allocation instrumenter agent.
    try {
      benchmarkMethod.invoke(benchmark, 1);
    } catch (IllegalAccessException impossible) {
      throw new AssertionError(impossible);
    } catch (InvocationTargetException e) {
      Throwable userException = e.getCause();
      propagateIfInstanceOf(userException, SkipThisScenarioException.class);
      throw new UserCodeException(userException);
    }
  }

  @Override
  public ImmutableSet<String> instrumentOptions() {
    return ImmutableSet.of(ALLOCATION_AGENT_JAR_OPTION);
  }

  private static Optional<File> findAllocationInstrumentJarOnClasspath() throws IOException {
    for (File file : JarFinder.findJarFiles(ClassLoader.getSystemClassLoader())) {
      JarFile jarFile = null;
      try {
        jarFile = new JarFile(file);
        Manifest manifest = jarFile.getManifest();
        if ((manifest != null)
            && AllocationInstrumenter.class.getName().equals(
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
   * This instrument's worker requires the allocationinstrumenter agent jar, specified
   * on the worker VM's command line with "-javaagent:[jarfile]".
   */
  @Override ImmutableSet<String> getExtraCommandLineArgs() {
    String agentJar = options.get("allocationAgentJar");
    if (Strings.isNullOrEmpty(agentJar)) {
      try {
        Optional<File> instrumentJar = findAllocationInstrumentJarOnClasspath();
        // TODO(gak): bundle up the allocation jar and unpack it if it's not on the classpath
        if (instrumentJar.isPresent()) {
          agentJar = instrumentJar.get().getAbsolutePath();
        }
      } catch (IOException e) {
        logger.log(SEVERE,
            "An exception occurred trying to locate the allocation agent jar on the classpath", e);
      }
    }
    // TODO(schmoe): what can we do to verify that agentJar is correct? Or to get a nicer
    // error message when it's not?
    if (Strings.isNullOrEmpty(agentJar) || !new File(agentJar).exists()) {
      throw new IllegalStateException("Can't find required allocationinstrumenter agent jar");
    }
    // Add microbenchmark args to minimize differences in the output
    return new ImmutableSet.Builder<String>()
        .addAll(super.getExtraCommandLineArgs())
        .add("-javaagent:" + agentJar)
        .build();
  }

  @Override
  public Class<? extends Worker> workerClass() {
    return AllocationWorker.class;
  }

  @Override
  MeasurementCollectingVisitor getMeasurementCollectingVisitor() {
    return new Instrument.DefaultMeasurementCollectingVisitor(ImmutableSet.of("bytes", "objects"));
  }
}
