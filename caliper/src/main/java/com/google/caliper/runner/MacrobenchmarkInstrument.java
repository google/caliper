/*
 * Copyright (C) 2013 Google Inc.
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

import static com.google.caliper.runner.CommonInstrumentOptions.MEASUREMENTS_OPTION;
import static com.google.caliper.runner.CommonInstrumentOptions.WARMUP_OPTION;
import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkState;
import static com.google.common.base.Throwables.propagateIfInstanceOf;
import static java.util.concurrent.TimeUnit.NANOSECONDS;

import com.google.caliper.api.AfterRep;
import com.google.caliper.api.BeforeRep;
import com.google.caliper.api.Macrobenchmark;
import com.google.caliper.api.SkipThisScenarioException;
import com.google.caliper.bridge.AbstractLogMessageVisitor;
import com.google.caliper.bridge.GcLogMessage;
import com.google.caliper.bridge.HotspotLogMessage;
import com.google.caliper.bridge.StartMeasurementLogMessage;
import com.google.caliper.bridge.StopMeasurementLogMessage;
import com.google.caliper.model.Measurement;
import com.google.caliper.util.ShortDuration;
import com.google.caliper.util.Stderr;
import com.google.caliper.util.Util;
import com.google.caliper.worker.MacrobenchmarkWorker;
import com.google.caliper.worker.Worker;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import com.google.inject.Inject;

import java.io.PrintWriter;
import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;

/**
 * An experimental instrument that measures {@link Macrobenchmark} invocations.
 */
public final class MacrobenchmarkInstrument extends Instrument {
  private final PrintWriter stderr;
  private final ShortDuration nanoTimeGranularity;

  @Inject MacrobenchmarkInstrument(@Stderr PrintWriter stderr,
      @NanoTimeGranularity ShortDuration nanoTimeGranularity) {
    this.stderr = stderr;
    this.nanoTimeGranularity = nanoTimeGranularity;
  }

  @Override
  protected ImmutableSet<String> instrumentOptions() {
    // TODO(gak): we need some way to GC since that'll probably be an issue, but every rep isn't
    // feasible. add GC_EVERY_N or something like that
    return ImmutableSet.of(WARMUP_OPTION, MEASUREMENTS_OPTION);
  }

  @Override
  public boolean isBenchmarkMethod(Method method) {
    return method.isAnnotationPresent(Macrobenchmark.class);
  }

  @Override
  public Instrumentation createInstrumentation(Method benchmarkMethod)
      throws InvalidBenchmarkException {
    checkArgument(isBenchmarkMethod(benchmarkMethod));
    Class<?>[] parameterTypes = benchmarkMethod.getParameterTypes();
    if (!Arrays.equals(parameterTypes, new Class<?>[] {})) {
      throw new InvalidBenchmarkException(
          "Macrobenchmark methods must not have parameters: " + benchmarkMethod.getName());
    }

    // Static technically doesn't hurt anything, but it's just the completely wrong idea
    if (Util.isStatic(benchmarkMethod)) {
      throw new InvalidBenchmarkException(
          "Macrobenchmark methods must not be static: " + benchmarkMethod.getName());
    }
    return new MacrobenchmarkInstrumentation(benchmarkMethod);
  }

  private final class MacrobenchmarkInstrumentation extends Instrumentation {
    MacrobenchmarkInstrumentation(Method benchmarkMethod) {
      super(benchmarkMethod);
    }

    @Override
    public void dryRun(Object benchmark) throws InvalidBenchmarkException {
      ImmutableSet<Method> beforeRepMethods =
          getAnnotatedMethods(benchmarkMethod.getDeclaringClass(), BeforeRep.class);
      ImmutableSet<Method> afterRepMethods =
          getAnnotatedMethods(benchmarkMethod.getDeclaringClass(), AfterRep.class);
      try {
        for (Method beforeRepMethod : beforeRepMethods) {
          beforeRepMethod.invoke(benchmark);
        }
        try {
          benchmarkMethod.invoke(benchmark);
        } finally {
          for (Method afterRepMethod : afterRepMethods) {
            afterRepMethod.invoke(benchmark);
          }
        }
      } catch (IllegalAccessException e) {
        throw new AssertionError(e);
      } catch (InvocationTargetException e) {
        Throwable userException = e.getCause();
        propagateIfInstanceOf(userException, SkipThisScenarioException.class);
        throw new UserCodeException(userException);
      }
    }

    @Override
    public Class<? extends Worker> workerClass() {
      return MacrobenchmarkWorker.class;
    }
  }

  private static ImmutableSet<Method> getAnnotatedMethods(Class<?> clazz,
      Class<? extends Annotation> annotationClass) {
    Method[] methods = clazz.getDeclaredMethods();
    ImmutableSet.Builder<Method> builder = ImmutableSet.builder();
    for (Method method : methods) {
      if (method.isAnnotationPresent(annotationClass)) {
        builder.add(method);
      }
    }
    return builder.build();
  }

  @Override
  MeasurementCollectingVisitor getMeasurementCollectingVisitor() {
    return new RuntimeMeasurementCollector(Integer.parseInt(options.get(MEASUREMENTS_OPTION)),
        ShortDuration.valueOf(options.get(WARMUP_OPTION)));
  }

  private final class RuntimeMeasurementCollector extends AbstractLogMessageVisitor
      implements MeasurementCollectingVisitor {
    int measurementsPerTrial;
    final ShortDuration warmup;
    final List<Measurement> measurements;
    boolean warnedAboutGc = false;
    boolean warnedAboutJit = false;
    boolean warnedAboutTimingJit = false;
    boolean timing = false;
    ShortDuration elapsedWarmup = ShortDuration.zero();

    RuntimeMeasurementCollector(int measurementsPerTrial, ShortDuration warmup) {
      this.measurementsPerTrial = measurementsPerTrial;
      this.measurements = Lists.newArrayListWithCapacity(measurementsPerTrial);
      this.warmup = warmup;
    }

    @Override
    public void visit(StartMeasurementLogMessage logMessage) {
      checkState(!timing);
      timing = true;
    }

    @Override public void visit(GcLogMessage logMessage) {
      // TODO(gak): account for the duration of the GC and figure out whether or not it matters
      if (!isInWarmup() && timing && !warnedAboutGc) {
        stderr.println("WARNING: GC occurred during timing. "
            + "Depending on the scope of the benchmark, this might significantly impact results. "
            + "Consider running with a larger heap size.");
        warnedAboutGc = true;
      }
    }

    @Override
    public void visit(HotspotLogMessage logMessage) {
      if (!isInWarmup()) {
        if (timing && !warnedAboutTimingJit) {
          stderr.println("WARNING: Hotspot compilation occurred during timing. "
              + "Depending on the scope of the benchmark, this might significantly impact results. "
              + "Consider running with a longer warmup.");
          warnedAboutTimingJit = true;
        } else if (!warnedAboutJit) {
          stderr.println(
              "WARNING: Hotspot compilation occurred after warmup, but outside of timing. "
              + "Depending on the scope of the benchmark, this might significantly impact results. "
              + "Consider running with a longer warmup.");
          warnedAboutJit = true;
        }
      }
    }

    @Override
    public void visit(StopMeasurementLogMessage logMessage) {
      checkState(timing);
      ImmutableList<Measurement> newMeasurements = logMessage.measurements();
      for (Measurement measurement : newMeasurements) {
        checkArgument("ns".equals(measurement.value().unit()));
        double nanos = measurement.value().magnitude() / measurement.weight();
        if ((nanos / 1000) < nanoTimeGranularity.to(NANOSECONDS)) {
          ShortDuration runtime = ShortDuration.of(BigDecimal.valueOf(nanos), NANOSECONDS);
          throw new TrialFailureException(String.format(
              "This experiment requires a microbenchmark. "
                  + "The granularity of the timer (%s) "
                  + "is greater than 0.1%% of the measured runtime (%s). "
                  + "Use the microbenchmark instrument for accurate measurements.%n",
                      nanoTimeGranularity, runtime));
        }
      }
      if (isInWarmup()) {
        for (Measurement measurement : newMeasurements) {
          // TODO(gak): eventually we will need to resolve different units
          elapsedWarmup = elapsedWarmup.plus(
              ShortDuration.of(BigDecimal.valueOf(measurement.value().magnitude()), NANOSECONDS));
        }
      } else {
        this.measurements.addAll(newMeasurements);
      }
      timing = false;
    }

    boolean isInWarmup() {
      return elapsedWarmup.compareTo(warmup) < 0;
    }

    @Override public boolean isDoneCollecting() {
      return measurements.size() >= measurementsPerTrial;
    }

    @Override
    public ImmutableList<Measurement> getMeasurements() {
      return ImmutableList.copyOf(measurements);
    }
  }
}
