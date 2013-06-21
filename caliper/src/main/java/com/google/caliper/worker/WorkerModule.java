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

import static com.google.common.base.Charsets.UTF_8;

import com.google.caliper.Param;
import com.google.caliper.bridge.WorkerSpec;
import com.google.common.base.Ticker;
import com.google.common.collect.ImmutableMap;
import com.google.common.net.InetAddresses;
import com.google.inject.AbstractModule;
import com.google.inject.Key;
import com.google.inject.Provides;
import com.google.inject.Singleton;

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.Socket;
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
  private final int port;
  private final ImmutableMap<String, String> workerOptions;

  @SuppressWarnings("unchecked")
  WorkerModule(WorkerSpec workerSpec) {
    try {
      this.workerClass = Class.forName(workerSpec.workerClassName).asSubclass(Worker.class);
    } catch (ClassNotFoundException e) {
      throw new AssertionError("classes referenced in the runner are always present");
    }
    this.port = workerSpec.port;
    this.workerOptions = workerSpec.workerOptions;
  }

  @Override protected void configure() {
    bind(Worker.class).to(workerClass);
    bind(Ticker.class).toInstance(Ticker.systemTicker());
    bind(WorkerEventLog.class);
    if (Boolean.valueOf(workerOptions.get("trackAllocations"))) {
      bind(AllocationRecorder.class).to(AllAllocationsRecorder.class);
    } else {
      bind(AllocationRecorder.class).to(AggregateAllocationsRecorder.class);
    }
    bind(Random.class);
    bind(new Key<Map<String, String>>(WorkerOptions.class) {}).toInstance(workerOptions);
  }

  @Provides @Singleton PrintWriter providePrintWriter() throws IOException {
    final Socket socket = new Socket(InetAddresses.forString("127.0.0.1"), port);
    // close the socket when the worker exits
    Runtime.getRuntime().addShutdownHook(new Thread() {
      @Override
      public void run() {
        try {
          socket.close();
        } catch (IOException e) {
          // nothing we can do
        }
      }
    });
    return new PrintWriter(new OutputStreamWriter(socket.getOutputStream(), UTF_8), true);
  }
}
