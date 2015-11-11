/*
 * Copyright (C) 2012 Google Inc.
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

import com.google.caliper.Param;
import com.google.caliper.bridge.WorkerSpec;
import com.google.caliper.runner.Running;
import com.google.caliper.util.InvalidCommandException;
import com.google.caliper.util.Util;
import com.google.common.base.Ticker;
import com.google.common.collect.ImmutableMap;
import dagger.MapKey;
import dagger.Module;
import dagger.Provides;
import dagger.Provides.Type;

import java.util.Map;
import java.util.Random;

import javax.inject.Provider;

/**
 * Binds classes necessary for the worker. Also manages the injection of {@link Param parameters}
 * from the {@link WorkerSpec} into the benchmark.
 *
 * <p>TODO(gak): Ensure that each worker only has bindings for the objects it needs and not the
 * objects required by different workers. (i.e. don't bind a Ticker if the worker is an allocation
 * worker).
 */
@Module
final class WorkerModule {
  private final Class<? extends Worker> workerClass;
  private final ImmutableMap<String, String> workerOptions;

  private final Class<?> benchmarkClassObject;

  WorkerModule(WorkerSpec workerSpec) throws ClassNotFoundException {
    this.workerClass = workerSpec.workerClass.asSubclass(Worker.class);
    this.workerOptions = workerSpec.workerOptions;

    benchmarkClassObject = Util.loadClass(workerSpec.benchmarkSpec.className());
  }

  @Provides
  @Running.BenchmarkClass
  Class<?> provideBenchmarkClassObject() {
    return benchmarkClassObject;
  }

  @Provides
  Worker provideWorker(Map<Class<? extends Worker>, Provider<Worker>> availableWorkers) {
    Provider<Worker> workerProvider = availableWorkers.get(workerClass);
    if (workerProvider == null) {
      throw new InvalidCommandException("%s is not a supported worker (%s).",
          workerClass, availableWorkers);
    }
    return workerProvider.get();
  }

  /**
   * Specifies the {@link Class} object to use as a key in the map of available
   * {@link Worker workers} passed to {@link #provideWorker(Map)}.
   */
  @MapKey(unwrapValue = true)
  public @interface WorkerClassKey {
    Class<? extends Worker> value();
  }

  @Provides(type = Type.MAP)
  @WorkerClassKey(ArbitraryMeasurementWorker.class)
  static Worker provideArbitraryMeasurementWorker(ArbitraryMeasurementWorker impl) {
    return impl;
  }

  @Provides(type = Type.MAP)
  @WorkerClassKey(MicrobenchmarkAllocationWorker.class)
  static Worker provideMicrobenchmarkAllocationWorker(MicrobenchmarkAllocationWorker impl) {
    return impl;
  }

  @Provides(type = Type.MAP)
  @WorkerClassKey(MacrobenchmarkWorker.class)
  static Worker provideMacrobenchmarkWorker(MacrobenchmarkWorker impl) {
    return impl;
  }

  @Provides(type = Type.MAP)
  @WorkerClassKey(MacrobenchmarkAllocationWorker.class)
  static Worker provideMacrobenchmarkAllocationWorker(MacrobenchmarkAllocationWorker impl) {
    return impl;
  }

  @Provides(type = Type.MAP)
  @WorkerClassKey(RuntimeWorker.Micro.class)
  static Worker provideRuntimeWorkerMicro(RuntimeWorker.Micro impl) {
    return impl;
  }

  @Provides(type = Type.MAP)
  @WorkerClassKey(RuntimeWorker.Pico.class)
  static Worker provideRuntimeWorkerPico(RuntimeWorker.Pico impl) {
    return impl;
  }

  @Provides
  static Ticker provideTicker() {
    return Ticker.systemTicker();
  }

  @Provides
  AllocationRecorder provideAllocationRecorder(
      Provider<AllAllocationsRecorder> allAllocationsRecorderProvider,
      Provider<AggregateAllocationsRecorder> aggregateAllocationsRecorderProvider) {

    return Boolean.valueOf(workerOptions.get("trackAllocations"))
        ? allAllocationsRecorderProvider.get()
        : aggregateAllocationsRecorderProvider.get();
  }

  @Provides
  static Random provideRandom() {
    return new Random();
  }

  @Provides
  @WorkerOptions
  Map<String, String> provideWorkerOptions() {
    return workerOptions;
  }
}
