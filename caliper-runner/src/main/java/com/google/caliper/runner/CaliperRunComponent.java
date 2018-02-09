/*
 * Copyright (C) 2015 Google Inc.
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

import com.google.caliper.model.BenchmarkClassModel;
import dagger.BindsInstance;
import dagger.Subcomponent;

/** The component for a {@link CaliperRun}. */
@RunScoped
@Subcomponent(
  modules = {
    CaliperRunModule.class,
    HostModule.class,
    NanoTimeGranularityModule.class,
    InstrumentModule.class,
    ResultProcessorModule.class
  }
)
interface CaliperRunComponent {

  /** Returns the Caliper benchmark run. */
  CaliperRun getCaliperRun();

  /** Builder for {@link CaliperRunComponent}. */
  @Subcomponent.Builder
  interface Builder {
    @BindsInstance
    Builder benchmarkClassModel(BenchmarkClassModel model);

    /**  Builds a new {@link CaliperRunComponent}. */
    CaliperRunComponent build();
  }
}
