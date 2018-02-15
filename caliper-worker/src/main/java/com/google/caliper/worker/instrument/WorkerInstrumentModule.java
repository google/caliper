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

package com.google.caliper.worker.instrument;

import com.google.caliper.bridge.ExperimentSpec;
import com.google.caliper.core.Running.Benchmark;
import com.google.caliper.core.Running.BenchmarkClass;
import com.google.caliper.core.Running.BenchmarkMethod;
import com.google.caliper.model.BenchmarkSpec;
import com.google.caliper.model.InstrumentType;
import com.google.caliper.util.InvalidCommandException;
import com.google.caliper.util.Util;
import com.google.common.base.Function;
import com.google.common.base.Ticker;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSortedMap;
import com.google.common.collect.Maps;
import com.google.common.primitives.Primitives;
import dagger.Binds;
import dagger.Module;
import dagger.Provides;
import dagger.Reusable;
import dagger.multibindings.IntoMap;
import java.lang.reflect.Method;
import java.util.Map;
import javax.inject.Provider;

/**
 * Binds classes necessary for the worker instruments. Also manages the injection of {@link
 * com.google.caliper.Param parameters} from the {@link ExperimentSpec} into the benchmark.
 *
 * <p>TODO(gak): Ensure that each worker only has bindings for the objects it needs and not the
 * objects required by different workers. (i.e. don't bind a Ticker if the worker is an allocation
 * worker).
 */
@Module
public abstract class WorkerInstrumentModule {

  @Provides
  static BenchmarkSpec provideBenchmarkSpec(ExperimentSpec experiment) {
    return experiment.benchmarkSpec();
  }

  @Provides
  @Benchmark
  static Object provideBenchmarkInstance(BenchmarkCreator creator) {
    return creator.createBenchmarkInstance();
  }

  @Provides
  @Reusable
  @BenchmarkMethod
  static Method provideBenchmarkMethod(
      ExperimentSpec experiment,
      BenchmarkSpec benchmarkSpec,
      @BenchmarkClass Class<?> benchmarkClass) {
    Method method =
        findBenchmarkMethod(
            benchmarkClass, benchmarkSpec.methodName(), experiment.methodParameterClasses());
    method.setAccessible(true);
    return method;
  }

  @Provides
  @BenchmarkMethod
  static String provideBenchmarkMethodName(@BenchmarkMethod Method benchmarkMethod) {
    return benchmarkMethod.getName();
  }

  @Provides
  @Benchmark
  static ImmutableSortedMap<String, String> provideUserParameters(BenchmarkSpec spec) {
    return spec.parameters();
  }

  @Provides
  static InstrumentType provideInstrumentType(ExperimentSpec experiment) {
    return experiment.instrumentType();
  }

  @Provides
  @WorkerInstrument.Options
  static Map<String, String> provideWorkerInstrumentOptions(ExperimentSpec experiment) {
    return experiment.workerInstrumentOptions();
  }

  @Provides
  static WorkerInstrument provideWorkerInstrument(
      InstrumentType instrumentType,
      Map<InstrumentType, Provider<WorkerInstrument>> availableWorkerInstruments) {
    Provider<WorkerInstrument> workerInstrumentProvider =
        availableWorkerInstruments.get(instrumentType);
    if (workerInstrumentProvider == null) {
      throw new InvalidCommandException(
          "%s is not a supported instrument type (%s).",
          instrumentType, availableWorkerInstruments);
    }
    return workerInstrumentProvider.get();
  }

  @Binds
  @IntoMap
  @InstrumentTypeKey(InstrumentType.ARBITRARY_MEASUREMENT)
  abstract WorkerInstrument bindArbitraryMeasurementWorkerInstrument(
      ArbitraryMeasurementWorkerInstrument impl);

  @Binds
  @IntoMap
  @InstrumentTypeKey(InstrumentType.RUNTIME_MACRO)
  abstract WorkerInstrument bindMacrobenchmarkWorkerInstrument(MacrobenchmarkWorkerInstrument impl);

  @Binds
  @IntoMap
  @InstrumentTypeKey(InstrumentType.RUNTIME_MICRO)
  abstract WorkerInstrument bindRuntimeWorkerInstrumentMicro(RuntimeWorkerInstrument.Micro impl);

  @Binds
  @IntoMap
  @InstrumentTypeKey(InstrumentType.RUNTIME_PICO)
  abstract WorkerInstrument bindRuntimeWorkerInstrumentPico(RuntimeWorkerInstrument.Pico impl);

  @Provides
  static Ticker provideTicker() {
    return Ticker.systemTicker();
  }

  private static Method findBenchmarkMethod(
      Class<?> benchmark, String methodName, ImmutableList<String> methodParameterClasses) {
    Class<?>[] params = new Class<?>[methodParameterClasses.size()];
    for (int i = 0; i < methodParameterClasses.size(); i++) {
      try {
        String typeName = methodParameterClasses.get(i);
        Class<?> primitiveType = PRIMITIVE_TYPES.get(typeName);
        if (primitiveType != null) {
          params[i] = primitiveType;
        } else {
          params[i] = Util.loadClass(typeName);
        }
      } catch (ClassNotFoundException e) {
        throw new RuntimeException(e);
      }
    }
    try {
      return benchmark.getDeclaredMethod(methodName, params);
    } catch (NoSuchMethodException e) {
      throw new RuntimeException(e);
    } catch (SecurityException e) {
      // assertion error?
      throw new RuntimeException(e);
    }
  }

  private static final ImmutableMap<String, Class<?>> PRIMITIVE_TYPES =
      Maps.uniqueIndex(
          Primitives.allPrimitiveTypes(),
          new Function<Class<?>, String>() {
            @Override
            public String apply(Class<?> input) {
              return input.getName();
            }
          });
}
