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

package com.google.caliper.runner.worker.targetinfo;

import com.google.caliper.bridge.TargetInfoLogMessage;
import com.google.caliper.runner.target.Target;
import com.google.caliper.runner.worker.WorkerRunner;
import com.google.caliper.runner.worker.WorkerScoped;
import dagger.BindsInstance;
import dagger.Subcomponent;

/** Component for creating a {@link WorkerRunner} for getting the info from a specific target. */
@WorkerScoped
@Subcomponent(modules = TargetInfoModule.class)
public interface TargetInfoComponent {
  WorkerRunner<TargetInfoLogMessage> workerRunner();

  /** Builder for the component. */
  @Subcomponent.Builder
  interface Builder {
    /** Binds the target to get the model from. */
    @BindsInstance
    Builder target(Target target);

    /** Builds a new component. */
    TargetInfoComponent build();
  }
}
