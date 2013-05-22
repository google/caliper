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
import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.util.Random;

/**
 * Binds classes necessary for the worker. Also manages the injection of {@link Param parameters}
 * from the {@link WorkerSpec} into the benchmark.
 */
final class WorkerModule extends AbstractModule {
  private final Class<? extends Worker> workerClass;
  private final String pipePath;

  @SuppressWarnings("unchecked")
  WorkerModule(WorkerSpec workerSpec) {
    try {
      this.workerClass = Class.forName(workerSpec.workerClassName).asSubclass(Worker.class);
    } catch (ClassNotFoundException e) {
      throw new AssertionError("classes referenced in the runner are always present");
    }
    this.pipePath = workerSpec.pipePath;
  }

  @Override protected void configure() {
    bind(Worker.class).to(workerClass);
    bind(Ticker.class).toInstance(Ticker.systemTicker());
    bind(WorkerEventLog.class);
    bind(Random.class);
  }

  @Provides @Singleton PrintWriter providePrintWriter() throws FileNotFoundException {
    PrintWriter printWriter = new PrintWriter(
        new OutputStreamWriter(new FileOutputStream(new File(pipePath)), UTF_8), true);
    printWriter.println();  // prime the pipe
    printWriter.flush();
    return printWriter;
  }
}
