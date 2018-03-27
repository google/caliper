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

import com.google.caliper.runner.options.OptionsModule;
import com.google.caliper.util.OutputModule;
import dagger.Component;
import javax.inject.Singleton;

/** JVM implementation of {@link CaliperRunnerFactory}. */
@Singleton
@Component(
  modules = {
    CaliperRunnerModule.class,
    JvmRunnerModule.class
  }
)
interface JvmCaliperRunnerComponent extends CaliperRunnerFactory {

  @Component.Builder
  abstract class Builder {
    abstract Builder optionsModule(OptionsModule optionsModule);

    abstract Builder outputModule(OutputModule outputModule);

    abstract JvmCaliperRunnerComponent build();
  }
}
