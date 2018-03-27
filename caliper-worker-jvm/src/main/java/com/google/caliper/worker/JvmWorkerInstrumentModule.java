/*
 * Copyright (C) 2016 Google Inc.
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

import com.google.caliper.model.InstrumentType;
import com.google.caliper.worker.instrument.InstrumentTypeKey;
import com.google.caliper.worker.instrument.WorkerInstrument;
import dagger.Binds;
import dagger.Module;
import dagger.Provides;
import dagger.multibindings.IntoMap;
import java.util.Map;
import javax.inject.Provider;

/** Module providing worker instruments that should only be present when running on the JVM. */
@Module
abstract class JvmWorkerInstrumentModule {
  private JvmWorkerInstrumentModule() {}

  @Binds
  @IntoMap
  @InstrumentTypeKey(InstrumentType.ALLOCATION_MICRO)
  abstract WorkerInstrument abstractMicrobenchmarkAllocationWorkerInstrument(
      MicrobenchmarkAllocationWorkerInstrument impl);

  @Binds
  @IntoMap
  @InstrumentTypeKey(InstrumentType.ALLOCATION_MACRO)
  abstract WorkerInstrument bindsMacrobenchmarkAllocationWorkerInstrument(
      MacrobenchmarkAllocationWorkerInstrument impl);

  @Provides
  static AllocationRecorder provideAllocationRecorder(
      @WorkerInstrument.Options Map<String, String> workerInstrumentOptions,
      Provider<AllAllocationsRecorder> allAllocationsRecorderProvider,
      Provider<AggregateAllocationsRecorder> aggregateAllocationsRecorderProvider) {
    return Boolean.valueOf(workerInstrumentOptions.get("trackAllocations"))
        ? allAllocationsRecorderProvider.get()
        : aggregateAllocationsRecorderProvider.get();
  }
}
