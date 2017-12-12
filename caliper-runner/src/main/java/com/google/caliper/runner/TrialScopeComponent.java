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

import dagger.Subcomponent;

/** Component for creating a {@linkplain TrialScoped trial-scoped} {@link ScheduledTrial}. */
@TrialScoped
@WorkerScoped
@Subcomponent(modules = {TrialModule.class, WorkerModule.class})
interface TrialScopeComponent {
  ScheduledTrial getScheduledTrial();

  /** Builder for {@link TrialScopeComponent}. */
  @Subcomponent.Builder
  interface Builder {
    /** Sets the {@link TrialModule} for the component to use. */
    Builder trialModule(TrialModule module);

    /** Builds a new {@link TrialScopeComponent}. */
    TrialScopeComponent build();
  }
}
