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

package com.google.caliper.worker;

import com.google.caliper.worker.instrument.WorkerInstrument;
import com.google.caliper.worker.instrument.WorkerInstrumentComponent;
import com.google.caliper.worker.instrument.WorkerInstrumentModule;
import dagger.Subcomponent;

/** Component that configures {@link WorkerInstrument}s for Android VMs. */
@Subcomponent(modules = WorkerInstrumentModule.class)
interface AndroidWorkerInstrumentComponent extends WorkerInstrumentComponent {
  /** Builder for {@link AndroidWorkerInstrumentComponent}. */
  @Subcomponent.Builder
  interface Builder extends WorkerInstrumentComponent.Builder {}
}
