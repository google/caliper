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

package com.google.caliper.runner;

import com.google.common.collect.ImmutableSet;
import dagger.BindsInstance;
import dagger.Subcomponent;
import java.util.Set;

/**
 * Component for dry-running experiments on a worker and getting the results.
 *
 * <p>Dry-runs for all experiments that are to be run on a single target are done in a single worker
 * process so as to avoid the overhead of creating potentially hundreds of worker processes.
 *
 * @author Colin Decker
 */
@WorkerScoped
@Subcomponent(modules = {DryRunModule.class, WorkerModule.class})
interface DryRunComponent {
  WorkerRunner<ImmutableSet<Experiment>> workerRunner();

  /** Builder for {@link DryRunComponent}. */
  @Subcomponent.Builder
  interface Builder {
    /** Binds the set of experiments to be dry-run. */
    @BindsInstance
    Builder experiments(Set<Experiment> experiment);

    /** Builds a new {@link DryRunComponent}. */
    DryRunComponent build();
  }
}
