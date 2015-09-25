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

package dk.ilios.caliperx.worker;

import dk.ilios.caliperx.Param;
import dk.ilios.caliperx.bridge.WorkerSpec;
import com.google.common.base.Ticker;
import com.google.common.collect.ImmutableMap;
import com.google.inject.AbstractModule;
import com.google.inject.Key;

import java.util.Map;
import java.util.Random;

/**
 * Binds classes necessary for the worker. Also manages the injection of {@link Param parameters}
 * from the {@link WorkerSpec} into the benchmark.
 * 
 * <p>TODO(gak): Ensure that each worker only has bindings for the objects it needs and not the
 * objects required by different workers. (i.e. don't bind a Ticker if the worker is an allocation
 * worker).
 */
final class WorkerModule extends AbstractModule {
  private final Class<? extends Worker> workerClass;
  private final ImmutableMap<String, String> workerOptions;

  WorkerModule(WorkerSpec workerSpec) {
    this.workerClass = workerSpec.workerClass.asSubclass(Worker.class);
    this.workerOptions = workerSpec.workerOptions;
  }

  @Override protected void configure() {
    bind(Worker.class).to(workerClass);
    bind(Ticker.class).toInstance(Ticker.systemTicker());
    if (Boolean.valueOf(workerOptions.get("trackAllocations"))) {
      bind(AllocationRecorder.class).to(AllAllocationsRecorder.class);
    } else {
      bind(AllocationRecorder.class).to(AggregateAllocationsRecorder.class);
    }
    bind(Random.class);
    bind(new Key<Map<String, String>>(WorkerOptions.class) {}).toInstance(workerOptions);
  }
}
