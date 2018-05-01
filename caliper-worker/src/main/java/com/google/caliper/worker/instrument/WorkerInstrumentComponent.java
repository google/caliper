/*
 * Copyright (C) 2018 Google Inc.
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
import dagger.BindsInstance;

/** Base interface for components that configure {@link WorkerInstrument}s. */
public interface WorkerInstrumentComponent {

  /** Returns the {@link WorkerInstrument} configured by this component. */
  WorkerInstrument instrument();

  /** Builder for {@link WorkerInstrumentComponent}. */
  public interface Builder {
    /** Binds the experiment the instrument should be set up to run. */
    @BindsInstance
    Builder experiment(ExperimentSpec experiment);

    /** Builds the component. */
    WorkerInstrumentComponent build();
  }
}
