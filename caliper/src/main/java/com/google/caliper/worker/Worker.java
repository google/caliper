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
package com.google.caliper.worker;

import com.google.caliper.model.Measurement;
import com.google.caliper.runner.Running.AfterExperimentMethods;
import com.google.caliper.runner.Running.BeforeExperimentMethods;
import com.google.common.collect.ImmutableSet;

import java.lang.reflect.Method;

import javax.inject.Inject;

/**
 * A {@link Worker} collects measurements on behalf of a particular Instrument.
 */
public abstract class Worker {
  @Inject
  @BeforeExperimentMethods
  ImmutableSet<Method> beforeExperimentMethods;
  
  @Inject
  @AfterExperimentMethods
  ImmutableSet<Method> afterExperimentMethods;
  
  protected final Method benchmarkMethod;
  protected final Object benchmark;
  
  protected Worker(Object benchmark, Method method) {
    this.benchmark = benchmark;
    this.benchmarkMethod = method;
  }

  /** Initializes the benchmark object. */
  final void setUpBenchmark() throws Exception {
    for (Method method : beforeExperimentMethods) {
      method.invoke(benchmark);
    }
  }

  /** Called once before all measurements but after benchmark setup. */
  public void bootstrap() throws Exception {}

  /**
   * Called immediately before {@link #measure()}.
   *
   * @param inWarmup whether we are in warmup, or taking real measurements. Used by
   *                 some implementations to skip forcing GC to make warmup faster.
   */
  public void preMeasure(boolean inWarmup) throws Exception {}

  /** Called immediately after {@link #measure()}. */
  public void postMeasure() throws Exception {}

  /** Template method for workers that produce multiple measurements. */
  public abstract Iterable<Measurement> measure() throws Exception;

  /** Tears down the benchmark object. */
  final void tearDownBenchmark() throws Exception {
    for (Method method : afterExperimentMethods) {
      method.invoke(benchmark);
    }
  }
}
