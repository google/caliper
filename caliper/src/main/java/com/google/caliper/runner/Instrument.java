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

import com.google.caliper.Benchmark;
import com.google.caliper.bridge.AbstractLogMessageVisitor;
import com.google.caliper.bridge.LogMessageVisitor;
import com.google.caliper.bridge.StopTimingLogMessage;
import com.google.caliper.model.InstrumentSpec;
import com.google.caliper.model.Measurement;
import com.google.caliper.util.ShortDuration;
import com.google.caliper.util.Util;
import com.google.caliper.worker.Worker;
import com.google.common.base.Predicates;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ListMultimap;
import com.google.common.collect.Maps;
import com.google.inject.Inject;

import java.lang.reflect.Method;
import java.util.Arrays;

public abstract class Instrument {
  protected ImmutableMap<String, String> options;

  @Inject void setOptions(@InstrumentOptions ImmutableMap<String, String> options) {
    this.options = ImmutableMap.copyOf(
        Maps.filterKeys(options, Predicates.in(instrumentOptions())));
  }

  public ShortDuration estimateRuntimePerTrial() {
    throw new UnsupportedOperationException();
  }

  public abstract boolean isBenchmarkMethod(Method method);

  // TODO: make BenchmarkMethod more abstract, not necessarily tied persistently to a Method (even
  // though the presence of a particular method is what probably triggers its recognition/creation
  // in the first place?), and give it an invoke() method.

  public abstract BenchmarkMethod createBenchmarkMethod(
      BenchmarkClass benchmarkClass, Method method) throws InvalidBenchmarkException;

  public abstract void dryRun(Benchmark benchmark, BenchmarkMethod method)
      throws InvalidBenchmarkException;

  public final ImmutableMap<String, String> options() {
    return options;
  }

  /**
   * Return the subset of options (and possibly a transformation thereof) to be used in the worker.
   * Returns all instrument options by default.
   */
  public ImmutableMap<String, String> workerOptions() {
    return options;
  }

  public abstract Class<? extends Worker> workerClass();

  final InstrumentSpec getSpec() {
    return new InstrumentSpec.Builder()
        .instrumentClass(getClass())
        .addAllOptions(options())
        .build();
  }

  /**
   * Defines the list of options applicable to this instrument. Implementations that use options
   * will need to override this method.
   */
  protected ImmutableSet<String> instrumentOptions() {
    return ImmutableSet.of();
  }

  /**
   * Some default JVM args to keep worker VMs somewhat predictable.
   */
  static final ImmutableSet<String> JVM_ARGS = ImmutableSet.of(
      // do compilation serially
      "-Xbatch",
      // make sure compilation doesn't run in parallel with itself
      "-XX:CICompilerCount=1",
      // ensure the parallel garbage collector
      "-XX:+UseParallelGC",
      // generate classes or don't, but do it immediately
      "-Dsun.reflect.inflationThreshold=0");

  /**
   * Returns some arguments that should be added to the command line when invoking
   * this instrument's worker.
   */
  ImmutableSet<String> getExtraCommandLineArgs() {
    return JVM_ARGS;
  }

  /**
   * Several instruments look for benchmark methods like {@code timeBlah(int reps)}; this is the
   * centralized code that identifies such methods.
   */
  public static boolean isTimeMethod(Method method) {
    return method.getName().startsWith("time") && Util.isPublic(method);
  }

  /**
   * For instruments that use {@link #isTimeMethod} to identify their methods, this method builds a
   * {@link BenchmarkMethod} appropriately.
   */
  public static BenchmarkMethod createBenchmarkMethodFromTimeMethod(
      BenchmarkClass benchmarkClass, Method timeMethod) throws InvalidBenchmarkException {

    checkArgument(isTimeMethod(timeMethod));
    if (!Arrays.equals(timeMethod.getParameterTypes(), new Class<?>[] {int.class})) {
      throw new InvalidBenchmarkException(
          "Microbenchmark methods must accept a single int parameter: " + timeMethod.getName());
    }

    // Static technically doesn't hurt anything, but it's just the completely wrong idea
    if (Util.isStatic(timeMethod)) {
      throw new InvalidBenchmarkException(
          "Microbenchmark methods must not be static: " + timeMethod.getName());
    }

    String methodName = timeMethod.getName();
    String shortName = methodName.substring("time".length());
    return new BenchmarkMethod(benchmarkClass, timeMethod, shortName);
  }

  abstract MeasurementCollectingVisitor getMeasurementCollectingVisitor();

  interface MeasurementCollectingVisitor extends LogMessageVisitor {
    boolean isDoneCollecting();
    ImmutableList<Measurement> getMeasurements();
  }

  /**
   * A default implementation of {@link MeasurementCollectingVisitor} that collects measurements for
   * pre-specified descriptions.
   */
  protected static final class DefaultMeasurementCollectingVisitor
      extends AbstractLogMessageVisitor implements MeasurementCollectingVisitor {
    static final int DEFAULT_NUMBER_OF_MEASUREMENTS = 9;
    final ImmutableSet<String> requiredDescriptions;
    final ListMultimap<String, Measurement> measurementsByDescription;
    final int requiredMeasurements;

    DefaultMeasurementCollectingVisitor(ImmutableSet<String> requiredDescriptions) {
      this(requiredDescriptions, DEFAULT_NUMBER_OF_MEASUREMENTS);
    }

    DefaultMeasurementCollectingVisitor(ImmutableSet<String> requiredDescriptions,
        int requiredMeasurements) {
      this.requiredDescriptions = requiredDescriptions;
      checkArgument(!requiredDescriptions.isEmpty());
      this.requiredMeasurements = requiredMeasurements;
      checkArgument(requiredMeasurements > 0);
      this.measurementsByDescription =
          ArrayListMultimap.create(requiredDescriptions.size(), requiredMeasurements);
    }

    @Override public void visit(StopTimingLogMessage logMessage) {
      for (Measurement measurement : logMessage.measurements()) {
        measurementsByDescription.put(measurement.description(), measurement);
      }
    }

    @Override public boolean isDoneCollecting() {
      for (String description : requiredDescriptions) {
        if (measurementsByDescription.get(description).size() < requiredMeasurements) {
          return false;
        }
      }
      return true;
    }

    @Override public ImmutableList<Measurement> getMeasurements() {
      return ImmutableList.copyOf(measurementsByDescription.values());
    }
  }
}
