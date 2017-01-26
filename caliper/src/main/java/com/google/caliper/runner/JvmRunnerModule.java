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

package com.google.caliper.runner;

import com.google.caliper.runner.ExperimentingRunnerModule.InstrumentClassKey;
import dagger.Binds;
import dagger.Module;
import dagger.Provides;
import dagger.multibindings.IntoMap;

/**
 * Module providing runner package dependencies that should only be present when running on the JVM.
 */
@Module
abstract class JvmRunnerModule {

  @Provides
  @IntoMap
  @InstrumentClassKey(AllocationInstrument.class)
  static Instrument provideAllocationInstrument() {
    return new AllocationInstrument();
  }

  /**
   * Binding that we need since we want to inject the {@code MainComponent} in a class, but {@code
   * MainComponent} isn't actually the component type.
   */
  @Binds
  abstract MainComponent bindMainComponent(JvmMainComponent component);
}
