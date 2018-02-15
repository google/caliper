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

import dagger.Component;
import javax.inject.Singleton;

/** Main class for running workers on Android VMs. */
@Singleton
@Component(modules = {WorkerModule.class, AndroidWorkerModule.class})
public abstract class WorkerMain implements WorkerComponent {

  public static void main(String[] args) throws Exception {
    Worker worker = DaggerWorkerMain.builder().args(args).build().worker();
    worker.run();
  }

  @Component.Builder
  interface Builder extends WorkerComponent.Builder {
    @Override
    WorkerMain build(); // to prevent Dagger warnings about the main() method
  }
}
