/*
 * Copyright (C) 2013 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */

package com.google.caliper.runner.instrument;

import static com.google.caliper.runner.instrument.CommonInstrumentOptions.GC_BEFORE_EACH_OPTION;
import static com.google.caliper.runner.instrument.CommonInstrumentOptions.MAX_WARMUP_WALL_TIME_OPTION;
import static com.google.caliper.runner.instrument.CommonInstrumentOptions.MEASUREMENTS_OPTION;
import static com.google.caliper.runner.instrument.CommonInstrumentOptions.WARMUP_OPTION;
import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static java.util.concurrent.TimeUnit.NANOSECONDS;

import com.google.caliper.Benchmark;
import com.google.caliper.api.Macrobenchmark;
import com.google.caliper.bridge.AbstractLogMessageVisitor;
import com.google.caliper.bridge.GcLogMessage;
import com.google.caliper.bridge.HotspotLogMessage;
import com.google.caliper.bridge.StartMeasurementLogMessage;
import com.google.caliper.bridge.StopMeasurementLogMessage;
import com.google.caliper.core.BenchmarkClassModel.MethodModel;
import com.google.caliper.core.InvalidBenchmarkException;
import com.google.caliper.model.InstrumentType;
import com.google.caliper.model.Measurement;
import com.google.caliper.runner.platform.Platform;
import com.google.caliper.runner.platform.SupportedPlatform;
import com.google.caliper.util.ShortDuration;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Optional;
import com.google.common.base.Stopwatch;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import java.lang.reflect.Modifier;
import java.math.BigDecimal;
import java.util.List;
import java.util.logging.Logger;
import javax.annotation.Nullable;
import javax.inject.Inject;

/** The instrument responsible for measuring the runtime of {@link Benchmark} methods. */
@SupportedPlatform({Platform.Type.JVM, Platform.Type.DALVIK})
public class RuntimeInstrument extends Instrument {
  private static final String SUGGEST_GRANULARITY_OPTION = "suggestGranularity";
  private static final String TIMING_INTERVAL_OPTION = "timingInterval";

  private static final Logger logger = Logger.getLogger(RuntimeInstrument.class.getName());

  private final ShortDuration nanoTimeGranularity;

  @VisibleForTesting
  @Inject
  public RuntimeInstrument(@NanoTimeGranularity ShortDuration nanoTimeGranularity) {
    this.nanoTimeGranularity = nanoTimeGranularity;
    setInstrumentName("runtime"); // default
  }

  @Override
  public boolean isBenchmarkMethod(MethodModel method) {
    return method.isAnnotationPresent(Benchmark.class)
        || BenchmarkMethods.isTimeMethod(method)
        || method.isAnnotationPresent(Macrobenchmark.class);
  }

  @Override
  protected ImmutableSet<String> instrumentOptions() {
    return ImmutableSet.of(
        WARMUP_OPTION,
        MAX_WARMUP_WALL_TIME_OPTION,
        TIMING_INTERVAL_OPTION,
        MEASUREMENTS_OPTION,
        GC_BEFORE_EACH_OPTION,
        SUGGEST_GRANULARITY_OPTION);
  }

  @Override
  public InstrumentedMethod createInstrumentedMethod(MethodModel benchmarkMethod)
      throws InvalidBenchmarkException {
    checkNotNull(benchmarkMethod);
    checkArgument(isBenchmarkMethod(benchmarkMethod));
    if (Modifier.isStatic(benchmarkMethod.modifiers())) {
      throw new InvalidBenchmarkException(
          "Benchmark methods must not be static: %s", benchmarkMethod.name());
    }
    try {
      switch (BenchmarkMethods.Type.of(benchmarkMethod)) {
        case MACRO:
          return new MacrobenchmarkInstrumentedMethod(benchmarkMethod);
        case MICRO:
          return new MicrobenchmarkInstrumentedMethod(benchmarkMethod);
        case PICO:
          return new PicobenchmarkInstrumentedMethod(benchmarkMethod);
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

  private class MacrobenchmarkInstrumentedMethod extends InstrumentedMethod {
    MacrobenchmarkInstrumentedMethod(MethodModel benchmarkMethod) {
      super(benchmarkMethod);
    }

    @Override
    public InstrumentType type() {
      return InstrumentType.RUNTIME_MACRO;
    }

    @Override
    public MeasurementCollectingVisitor getMeasurementCollectingVisitor() {
      return new SingleInvocationMeasurementCollector(
          Integer.parseInt(options.get(MEASUREMENTS_OPTION)),
          ShortDuration.valueOf(options.get(WARMUP_OPTION)),
          ShortDuration.valueOf(options.get(MAX_WARMUP_WALL_TIME_OPTION)));
    }
  }

  private abstract class RuntimeInstrumentedMethod extends InstrumentedMethod {
    RuntimeInstrumentedMethod(MethodModel method) {
      super(method);
    }

    @Override
    public ImmutableMap<String, String> workerOptions() {
      return ImmutableMap.of(
          TIMING_INTERVAL_OPTION + "Nanos",
          toNanosString(TIMING_INTERVAL_OPTION),
          GC_BEFORE_EACH_OPTION,
          options.get(GC_BEFORE_EACH_OPTION));
    }

    private String toNanosString(String optionName) {
      return String.valueOf(ShortDuration.valueOf(options.get(optionName)).to(NANOSECONDS));
    }

    @Override
    public MeasurementCollectingVisitor getMeasurementCollectingVisitor() {
      return new RepBasedMeasurementCollector(
          getMeasurementsPerTrial(),
          ShortDuration.valueOf(options.get(WARMUP_OPTION)),
          ShortDuration.valueOf(options.get(MAX_WARMUP_WALL_TIME_OPTION)));
    }

    @Override
    public Optional<String> validateMeasurements(
        Iterable<ImmutableList<Measurement>> trialResults) {
      if (!Boolean.parseBoolean(options.get(SUGGEST_GRANULARITY_OPTION))) {
        return Optional.absent();
      }
      boolean hasResults = false;
      // if any measurement takes less than this much time, then the benchmark shouldn't be promoted
      // so a MacroBenchmark.
      ShortDuration reasonableUpperBound = nanoTimeGranularity.times(1000);
      for (ImmutableList<Measurement> measurements : trialResults) {
        for (Measurement measurement : measurements) {
          hasResults = true;
          double nanos = measurement.value().magnitude() / measurement.weight();
          if (nanos < reasonableUpperBound.to(NANOSECONDS)) {
            // At least one measurement was faster than the upper bound, so don't warn
            return Optional.absent();
          }
        }
      }
      // If for some reason there are no measurements, don't issue a warning
      if (hasResults) {
        return Optional.of(
            String.format(
                "This benchmark does not require a microbenchmark. "
                    + "The granularity of the timer (%s) is less than 0.1%% of the fastest "
                    + "measured runtime across all experiments.",
                nanoTimeGranularity));
      }
      return Optional.absent();
    }
  }

  private class MicrobenchmarkInstrumentedMethod extends RuntimeInstrumentedMethod {
    MicrobenchmarkInstrumentedMethod(MethodModel benchmarkMethod) {
      super(benchmarkMethod);
    }

    @Override
    public InstrumentType type() {
      return InstrumentType.RUNTIME_MICRO;
    }
  }

  private int getMeasurementsPerTrial() {
    @Nullable String measurementsString = options.get(MEASUREMENTS_OPTION);
    int measurementsPerTrial =
        (measurementsString == null) ? 1 : Integer.parseInt(measurementsString);
    // TODO(gak): fail faster
    checkState(measurementsPerTrial > 0);
    return measurementsPerTrial;
  }

  private class PicobenchmarkInstrumentedMethod extends RuntimeInstrumentedMethod {
    PicobenchmarkInstrumentedMethod(MethodModel benchmarkMethod) {
      super(benchmarkMethod);
    }

    @Override
    public InstrumentType type() {
      return InstrumentType.RUNTIME_PICO;
    }
  }

  private abstract static class RuntimeMeasurementCollector extends AbstractLogMessageVisitor
      implements MeasurementCollectingVisitor {
    final int targetMeasurements;
    final ShortDuration warmup;
    final ShortDuration maxWarmupWallTime;
    final List<Measurement> measurements = Lists.newArrayList();
    ShortDuration elapsedWarmup = ShortDuration.zero();
    boolean measuring = false;
    boolean invalidateMeasurements = false;
    boolean notifiedAboutGc = false;
    boolean notifiedAboutJit = false;
    boolean notifiedAboutMeasuringJit = false;
    Stopwatch timeSinceStartOfTrial = Stopwatch.createUnstarted();
    final List<String> messages = Lists.newArrayList();

    RuntimeMeasurementCollector(
        int targetMeasurements,
        ShortDuration warmup,
        ShortDuration maxWarmupWallTime) {
      this.targetMeasurements = targetMeasurements;
      this.warmup = warmup;
      this.maxWarmupWallTime = maxWarmupWallTime;
    }

    @Override
    public void visit(GcLogMessage logMessage) {
      if (measuring && isWarmupComplete() && !notifiedAboutGc) {
        gcWhileMeasuring();
        notifiedAboutGc = true;
      }
    }

    abstract void gcWhileMeasuring();

    @Override
    public void visit(HotspotLogMessage logMessage) {
      if (isWarmupComplete()) {
        if (measuring && notifiedAboutMeasuringJit) {
          hotspotWhileMeasuring();
          notifiedAboutMeasuringJit = true;
        } else if (notifiedAboutJit) {
          hotspotWhileNotMeasuring();
          notifiedAboutJit = true;
        }
      }
    }

    abstract void hotspotWhileMeasuring();

    abstract void hotspotWhileNotMeasuring();

    @Override
    public void visit(StartMeasurementLogMessage logMessage) {
      checkState(!measuring);
      measuring = true;
      if (!timeSinceStartOfTrial.isRunning()) {
        timeSinceStartOfTrial.start();
      }
    }

    @Override
    public void visit(StopMeasurementLogMessage logMessage) {
      checkState(measuring);
      ImmutableList<Measurement> newMeasurements = logMessage.measurements();
      if (!isWarmupComplete()) {
        for (Measurement measurement : newMeasurements) {
          // TODO(gak): eventually we will need to resolve different units
          checkArgument("ns".equals(measurement.value().unit()));
          elapsedWarmup =
              elapsedWarmup.plus(
                  ShortDuration.of(
                      BigDecimal.valueOf(measurement.value().magnitude()), NANOSECONDS));
        }
      } else {
        if (!measuredWarmupDurationReached()) {
          messages.add(
              String.format(
                  "WARNING: Warmup was interrupted because it took longer than %s of wall-clock "
                      + "time. %s was spent in the benchmark method for warmup (normal warmup "
                      + "duration should be %s).",
                  maxWarmupWallTime, elapsedWarmup, warmup));
        }

        if (invalidateMeasurements) {
          logger.fine(String.format("Discarding %s as they were marked invalid.", newMeasurements));
        } else {
          this.measurements.addAll(newMeasurements);
        }
      }
      invalidateMeasurements = false;
      measuring = false;
    }

    @Override
    public ImmutableList<Measurement> getMeasurements() {
      return ImmutableList.copyOf(measurements);
    }

    boolean measuredWarmupDurationReached() {
      return elapsedWarmup.compareTo(warmup) >= 0;
    }

    @Override
    public boolean isWarmupComplete() {
      // Fast macro-benchmarks (up to tens of ms) need lots of measurements to reach 10s of
      // measured warmup time. Because of the per-measurement overhead of running @BeforeRep and
      // @AfterRep, warmup can take very long.
      //
      // To prevent this, we enforce a cap on the wall-clock time here.
      return measuredWarmupDurationReached()
          || timeSinceStartOfTrial.elapsed(MILLISECONDS) > maxWarmupWallTime.to(MILLISECONDS);
    }

    @Override
    public boolean isDoneCollecting() {
      return measurements.size() >= targetMeasurements;
    }

    @Override
    public ImmutableList<String> getMessages() {
      return ImmutableList.copyOf(messages);
    }
  }

  private static final class RepBasedMeasurementCollector extends RuntimeMeasurementCollector {
    RepBasedMeasurementCollector(
        int measurementsPerTrial,
        ShortDuration warmup,
        ShortDuration maxWarmupWallTime) {
      super(measurementsPerTrial, warmup, maxWarmupWallTime);
    }

    @Override
    void gcWhileMeasuring() {
      invalidateMeasurements = true;
      messages.add("ERROR: GC occurred during timing. Measurements were discarded.");
    }

    @Override
    void hotspotWhileMeasuring() {
      invalidateMeasurements = true;
      messages.add(
          "ERROR: Hotspot compilation occurred during timing: warmup is likely insufficent. "
              + "Measurements were discarded.");
    }

    @Override
    void hotspotWhileNotMeasuring() {
      messages.add(
          "WARNING: Hotspot compilation occurred after warmup, but outside of timing. "
              + "Results may be affected. Run with --verbose to see which method was compiled.");
    }
  }

  private static final class SingleInvocationMeasurementCollector
      extends RuntimeMeasurementCollector {

    SingleInvocationMeasurementCollector(
        int measurementsPerTrial,
        ShortDuration warmup,
        ShortDuration maxWarmupWallTime) {
      super(measurementsPerTrial, warmup, maxWarmupWallTime);
    }

    @Override
    void gcWhileMeasuring() {
      messages.add(
          "WARNING: GC occurred during timing. "
              + "Depending on the scope of the benchmark, this might significantly impact results. "
              + "Consider running with a larger heap size.");
    }

    @Override
    void hotspotWhileMeasuring() {
      messages.add(
          "WARNING: Hotspot compilation occurred during timing. "
              + "Depending on the scope of the benchmark, this might significantly impact results. "
              + "Consider running with a longer warmup.");
    }

    @Override
    void hotspotWhileNotMeasuring() {
      messages.add(
          "WARNING: Hotspot compilation occurred after warmup, but outside of timing. "
              + "Depending on the scope of the benchmark, this might significantly impact results. "
              + "Consider running with a longer warmup.");
    }
  }
}
