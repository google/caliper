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

package com.google.caliper.runner.worker.dryrun;

import static com.google.common.base.Preconditions.checkArgument;

import com.google.caliper.runner.experiment.Experiment;
import com.google.caliper.runner.target.Target;
import com.google.caliper.runner.worker.WorkerModule;
import com.google.caliper.runner.worker.WorkerProcessor;
import com.google.caliper.runner.worker.WorkerSpec;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import dagger.Binds;
import dagger.Module;
import dagger.Provides;
import dagger.Reusable;
import java.util.HashSet;
import java.util.Set;

/**
 * Configuration for a dry-run of an experiment.
 *
 * @author Colin Decker
 */
@Module(includes = WorkerModule.class)
abstract class DryRunModule {

  @Provides
  @Reusable
  static Target provideTarget(Set<Experiment> experiments) {
    checkArgument(!experiments.isEmpty(), "Dry-run component bound no experiments.");

    Set<Target> targets = new HashSet<>();
    for (Experiment experiment : experiments) {
      targets.add(experiment.target());
    }

    checkArgument(
        targets.size() == 1,
        "All dry-run component experiments should have the same target. "
            + "Experiments for %s different targets were found.",
        targets.size());

    return Iterables.getOnlyElement(targets);
  }

  @Binds
  abstract WorkerProcessor<ImmutableSet<Experiment>> bindWorkerProcessor(DryRunProcessor processor);

  @Binds
  abstract WorkerSpec bindWorkerSpec(DryRunSpec spec);
}
