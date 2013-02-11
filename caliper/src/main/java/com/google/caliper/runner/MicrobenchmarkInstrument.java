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

import static com.google.caliper.runner.CommonInstrumentOptions.GC_BEFORE_EACH_OPTION;
import static com.google.caliper.runner.CommonInstrumentOptions.MEASUREMENTS_OPTION;
import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkState;
import static com.google.common.base.Throwables.propagateIfInstanceOf;
import static java.util.concurrent.TimeUnit.NANOSECONDS;
import static java.util.logging.Level.WARNING;

import com.google.caliper.Benchmark;
import com.google.caliper.api.SkipThisScenarioException;
import com.google.caliper.bridge.AbstractLogMessageVisitor;
import com.google.caliper.bridge.GcLogMessage;
import com.google.caliper.bridge.HotspotLogMessage;
import com.google.caliper.bridge.StartTimingLogMessage;
import com.google.caliper.bridge.StopTimingLogMessage;
import com.google.caliper.model.Measurement;
import com.google.caliper.util.ShortDuration;
import com.google.caliper.worker.MicrobenchmarkWorker;
import com.google.caliper.worker.Worker;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

import javax.annotation.Nullable;

public final class MicrobenchmarkInstrument extends Instrument {
  private static final Logger logger = Logger.getLogger(MicrobenchmarkInstrument.class.getName());

  private static final String WARMUP_OPTION = "warmup";
  private static final String TIMING_INTERVAL_OPTION = "timingInterval";

  @Override public ShortDuration estimateRuntimePerTrial() {
    return ShortDuration.valueOf(options.get(TIMING_INTERVAL_OPTION))
        .times(Integer.valueOf(options.get(MEASUREMENTS_OPTION)))
        .plus(ShortDuration.valueOf(options.get(WARMUP_OPTION)));
  }

  @Override public boolean isBenchmarkMethod(Method method) {
    return Instrument.isTimeMethod(method);
  }

  @Override public BenchmarkMethod createBenchmarkMethod(BenchmarkClass benchmarkClass,
      Method method) throws InvalidBenchmarkException {

    return Instrument.createBenchmarkMethodFromTimeMethod(benchmarkClass, method);
  }

  @Override public void dryRun(Benchmark benchmark, BenchmarkMethod benchmarkMethod)
      throws UserCodeException {
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

  @Override public ImmutableSet<String> instrumentOptions() {
    return ImmutableSet.of(
        WARMUP_OPTION, TIMING_INTERVAL_OPTION, MEASUREMENTS_OPTION, GC_BEFORE_EACH_OPTION);
  }

  @Override public ImmutableMap<String, String> workerOptions() {
    return new ImmutableMap.Builder<String, String>()
        .put(TIMING_INTERVAL_OPTION + "Nanos", toNanosString(TIMING_INTERVAL_OPTION))
        .put(GC_BEFORE_EACH_OPTION, options.get(GC_BEFORE_EACH_OPTION))
        .build();
  }

  @Override public Class<? extends Worker> workerClass() {
    return MicrobenchmarkWorker.class;
  }

  private String toNanosString(String optionName) {
    return String.valueOf(ShortDuration.valueOf(options.get(optionName)).to(TimeUnit.NANOSECONDS));
  }

  @Override public boolean equals(Object object) {
    return object instanceof MicrobenchmarkInstrument; // currently this class is stateless.
  }

  @Override public int hashCode() {
    return 0x5FE89C3A;
  }

  @Override MeasurementCollectingVisitor getMeasurementCollectingVisitor() {
    return new RuntimeMeasurementCollector(getMeasurementsPerTrial(),
        ShortDuration.valueOf(options.get(WARMUP_OPTION)));
  }

  private int getMeasurementsPerTrial() {
    @Nullable String measurementsString = options.get(MEASUREMENTS_OPTION);
    int measurementsPerTrial = (measurementsString == null)
        ? 1
        : Integer.parseInt(measurementsString);
    // TODO(gak): fail faster
    checkState(measurementsPerTrial > 0);
    return measurementsPerTrial;
  }

  private static final class RuntimeMeasurementCollector extends AbstractLogMessageVisitor
      implements MeasurementCollectingVisitor {
    final int measurementsPerTrial;
    final ShortDuration warmup;
    final List<Measurement> measurements = Lists.newArrayList();
    boolean timing = false;
    boolean invalidMeasurements = false;
    ShortDuration elapsedWarmup = ShortDuration.zero();

    RuntimeMeasurementCollector(int measurementsPerTrial, ShortDuration warmup) {
      this.measurementsPerTrial = measurementsPerTrial;
      this.warmup = warmup;
    }

    boolean isInWarmup() {
      return elapsedWarmup.compareTo(warmup) < 0;
    }

    @Override
    public void visit(GcLogMessage logMessage) {
      if (timing && !isInWarmup()) {
        invalidMeasurements = true;
        logger.severe("GC occurred during timing.");
      }
    }

    @Override
    public void visit(HotspotLogMessage logMessage) {
      if (!isInWarmup()) {
        if (timing) {
          logger.severe(
              "Hotspot compilation occurred during timing. Warmup is likely insufficent.");
        } else {
          logger.log(WARNING, "Hotspot compilation occurred after warmup, but outside of timing. "
              + "Results may be affected. Run with --verbose to see which method was compiled.");
        }
      }
    }

    @Override
    public void visit(StartTimingLogMessage logMessage) {
      checkState(!timing);
      timing = true;
    }

    @Override
    public void visit(StopTimingLogMessage logMessage) {
      checkState(timing);
      ImmutableList<Measurement> newMeasurements = logMessage.measurements();
      if (isInWarmup()) {
        for (Measurement measurement : newMeasurements) {
          // TODO(gak): eventually we will need to resolve different units
          checkArgument("ns".equals(measurement.value().unit()));
          elapsedWarmup = elapsedWarmup.plus(
              ShortDuration.of(BigDecimal.valueOf(measurement.value().magnitude()), NANOSECONDS));
        }
      } else if (invalidMeasurements) {
        logger.fine(String.format("Discarding %s as they were marked invalid.", newMeasurements));
      } else {
        this.measurements.addAll(newMeasurements);
      }
      invalidMeasurements = false;
      timing = false;
    }

    @Override public boolean isDoneCollecting() {
      return measurements.size() >= measurementsPerTrial;
    }

    @Override public ImmutableList<Measurement> getMeasurements() {
      return ImmutableList.copyOf(measurements);
    }
  }
}
