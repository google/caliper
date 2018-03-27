/*
 * Copyright (C) 2013 Google Inc.
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

package com.google.caliper.runner.worker;

import com.google.common.util.concurrent.Service;
import dagger.Binds;
import dagger.Module;
import dagger.multibindings.IntoSet;

/** Configures the {@link WorkerOutputFactory}. */
@Module
public abstract class WorkerOutputModule {
  private WorkerOutputModule() {}

  @Binds
  @IntoSet
  abstract Service bindWorkerOutputFactoryService(WorkerOutputFactoryService impl);

  @Binds
  abstract WorkerOutputFactory bindWorkerOutputFactory(WorkerOutputFactoryService impl);
}
