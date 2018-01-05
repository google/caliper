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

import com.google.caliper.bridge.ExperimentSpec;
import com.google.caliper.core.Running;
import com.google.caliper.model.InstrumentType;
import com.google.caliper.util.InvalidCommandException;
import com.google.caliper.util.Util;
import com.google.common.base.Ticker;
import dagger.Binds;
import dagger.Module;
import dagger.Provides;
import dagger.multibindings.IntoMap;
import java.util.Map;
import java.util.Random;
import javax.inject.Provider;

/**
 * Binds classes necessary for the worker. Also manages the injection of {@link
 * com.google.caliper.Param parameters} from the {@link ExperimentSpec} into the benchmark.
 *
 * <p>TODO(gak): Ensure that each worker only has bindings for the objects it needs and not the
 * objects required by different workers. (i.e. don't bind a Ticker if the worker is an allocation
 * worker).
 */
@Module(includes = WorkerModule.OtherBindings.class)
final class WorkerModule {
  private final ExperimentSpec experiment;
  private final Class<?> benchmarkClassObject;

  WorkerModule(ExperimentSpec experiment) throws ClassNotFoundException {
    this.experiment = experiment;
    this.benchmarkClassObject = Util.loadClass(experiment.benchmarkSpec().className());
  }

  @Provides
  ExperimentSpec provideExperimentSpec() {
    return experiment;
  }

  @Provides
  @Running.BenchmarkClass
  Class<?> provideBenchmarkClassObject() {
    return benchmarkClassObject;
  }

  @Module
  abstract static class OtherBindings {

    @Provides
    static InstrumentType provideInstrumentType(ExperimentSpec experiment) {
      return experiment.instrumentType();
    }

    @Provides
    @WorkerOptions
    static Map<String, String> provideWorkerOptions(ExperimentSpec experiment) {
      return experiment.workerOptions();
    }

    @Provides
    static Worker provideWorker(
        InstrumentType instrumentType, Map<InstrumentType, Provider<Worker>> availableWorkers) {
      Provider<Worker> workerProvider = availableWorkers.get(instrumentType);
      if (workerProvider == null) {
        throw new InvalidCommandException(
            "%s is not a supported instrument type (%s).", instrumentType, availableWorkers);
      }
      return workerProvider.get();
    }

    @Binds
    @IntoMap
    @InstrumentTypeKey(InstrumentType.ARBITRARY_MEASUREMENT)
    abstract Worker bindArbitraryMeasurementWorker(ArbitraryMeasurementWorker impl);

    @Binds
    @IntoMap
    @InstrumentTypeKey(InstrumentType.RUNTIME_MACRO)
    abstract Worker provideMacrobenchmarkWorker(MacrobenchmarkWorker impl);

    @Binds
    @IntoMap
    @InstrumentTypeKey(InstrumentType.RUNTIME_MICRO)
    abstract Worker provideRuntimeWorkerMicro(RuntimeWorker.Micro impl);

    @Binds
    @IntoMap
    @InstrumentTypeKey(InstrumentType.RUNTIME_PICO)
    abstract Worker provideRuntimeWorkerPico(RuntimeWorker.Pico impl);

    @Provides
    static Ticker provideTicker() {
      return Ticker.systemTicker();
    }

    @Provides
    static Random provideRandom() {
      return new Random();
    }
  }
}
