// Copyright 2011 Google Inc. All Rights Reserved.

package com.google.caliper.worker;

import com.google.caliper.api.Benchmark;
import com.google.caliper.model.ArbitraryMeasurement;
import com.google.caliper.model.Measurement;
import com.google.caliper.util.Util;
import com.google.common.base.Ticker;
import com.google.common.collect.ImmutableList;

import java.lang.reflect.Method;
import java.util.Collection;
import java.util.Map;

/**
 * Worker for arbitrary measurements.
 *
 * @author schmoe@google.com (mike nonemacher)
 */
public final class ArbitraryMeasurementWorker implements Worker {
  // TODO(schmoe): replace this with Ticker.systemTicker() in guava r10
  private static final Ticker defaultTicker = new Ticker() {
    public long read() {
      return System.nanoTime();
    }
  };

  private final Ticker ticker;

  public ArbitraryMeasurementWorker() {
    this(defaultTicker);
  }

  public ArbitraryMeasurementWorker(Ticker ticker) {
    this.ticker = ticker;
  }

  @Override
  public Collection<Measurement> measure(Benchmark benchmark, String methodName,
      Map<String, String> optionsMap, WorkerEventLog log) throws Exception {

    Options options = new Options(optionsMap);
    Method method = benchmark.getClass().getDeclaredMethod(methodName);
    ArbitraryMeasurement annotation = method.getAnnotation(ArbitraryMeasurement.class);
    String unit = annotation.units();
    String description = annotation.description();

    // measure
    log.notifyMeasurementPhaseStarting();

    if (options.gcBeforeEach) {
      Util.forceGc();
    }

    log.notifyMeasurementStarting();
    double measured = (Double) method.invoke(benchmark);
    Measurement m = new Measurement();
    m.value = measured;
    m.weight = 1;
    m.unit = unit;
    m.description = description;
    log.notifyMeasurementEnding(m.value);

    return ImmutableList.of(m);
  }

  private static class Options {
    final boolean gcBeforeEach;

    Options(Map<String, String> options) {
      this.gcBeforeEach = Boolean.parseBoolean(options.get("gcBeforeEach"));
    }
  }
}
