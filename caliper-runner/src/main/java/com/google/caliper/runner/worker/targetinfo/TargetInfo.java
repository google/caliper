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

import com.google.auto.value.AutoValue;
import com.google.caliper.core.BenchmarkClassModel;
import com.google.caliper.model.Host;
import com.google.caliper.runner.target.Target;
import com.google.common.collect.ImmutableMap;
import java.util.Map;

/**
 * Information about the targets for the run. Specifically, the single, identical model of the
 * benchmark class that each of them should have produced, as well as a mapping from each target to
 * the {@link Host} properties for that target's device.
 */
@AutoValue
public abstract class TargetInfo {

  static TargetInfo create(BenchmarkClassModel model, Map<Target, Host> hosts) {
    return new AutoValue_TargetInfo(model, ImmutableMap.copyOf(hosts));
  }

  /**
   * Returns the benchmark class model. Each of the targets for the run must have produced an
   * identical model of the class to all of the other targets or we would have failed to get this
   * target info.
   */
  public abstract BenchmarkClassModel benchmarkClassModel();

  /** Returns the mapping of target to host device properties. */
  public abstract ImmutableMap<Target, Host> hosts();
}
