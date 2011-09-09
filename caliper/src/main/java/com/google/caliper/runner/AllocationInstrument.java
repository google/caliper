// Copyright 2011 Google Inc. All Rights Reserved.

package com.google.caliper.runner;

import static com.google.common.base.Throwables.propagateIfInstanceOf;

import com.google.caliper.api.Benchmark;
import com.google.caliper.api.SkipThisScenarioException;
import com.google.caliper.model.AllocationMeasurement;
import com.google.caliper.util.Util;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;

import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;

/**
 * {@link Instrument} that watches the memory allocations in an invocation of the
 * benchmark method and reports some statistic. The benchmark method must accept a
 * single int argument 'reps', which is the number of times to execute the guts of
 * the benchmark method, and it must be public and non-static.
 *
 * @author schmoe@google.com (mike nonemacher)
 */
abstract class AllocationInstrument extends Instrument {
  @Override public boolean isBenchmarkMethod(Method method) {
    return method.isAnnotationPresent(AllocationMeasurement.class);
  }

  @Override
  public BenchmarkMethod createBenchmarkMethod(BenchmarkClass benchmarkClass, Method method)
      throws InvalidBenchmarkException {

    if (!Arrays.equals(method.getParameterTypes(), new Class<?>[] {int.class})) {
      throw new InvalidBenchmarkException(
          "Allocation measurement methods must accept a single int parameter: " + method.getName());
    }

    // Static technically doesn't hurt anything, but it's just the completely wrong idea
    if (Util.isStatic(method)) {
      throw new InvalidBenchmarkException(
          "Allocation measurement methods must not be static: " + method.getName());
    }

    if (!Util.isPublic(method)) {
      throw new InvalidBenchmarkException(
          "Allocation measurement methods must be public: " + method.getName());
    }

    return new BenchmarkMethod(benchmarkClass, method, method.getName());
  }

  @Override
  public void dryRun(Benchmark benchmark, BenchmarkMethod benchmarkMethod)
      throws UserCodeException {

    // execute the benchmark method, but don't try to take any measurements, because this JVM
    // may not have the allocation instrumenter agent.
    Method m = benchmarkMethod.method();
    try {
      m.invoke(benchmark, 1);
    } catch (IllegalAccessException impossible) {
      throw new AssertionError(impossible);
    } catch (InvocationTargetException e) {
      Throwable userException = e.getCause();
      propagateIfInstanceOf(userException, SkipThisScenarioException.class);
      throw new UserCodeException(userException);
    }
  }

  /**
   * This instrument's worker requires the allocationinstrumenter agent jar, specified
   * on the worker VM's command line with "-javaagent:[jarfile]".
   */
  @Override Iterable<String> getExtraCommandLineArgs() {
    String agentJar = options.get("allocationAgentJar");
    // TODO(schmoe): what can we do to verify that agentJar is correct? Or to get a nicer
    // error message when it's not?
    if (agentJar == null || !new File(agentJar).exists()) {
      throw new IllegalStateException("Can't find required allocationinstrumenter agent jar");
    }
    return Iterables.concat(super.getExtraCommandLineArgs(),
        ImmutableList.of("-javaagent:" + agentJar));
  }
}
