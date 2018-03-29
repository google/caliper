/*
 * Copyright (C) 2017 Google Inc.
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

package com.google.caliper.bridge;

import com.google.auto.value.AutoValue;
import com.google.caliper.core.BenchmarkClassModel;
import com.google.common.collect.ImmutableMap;
import java.io.Serializable;
import java.util.Map;

/**
 * A log message containing the response to a {@link TargetInfoRequest}.
 *
 * @author Colin Decker
 */
@AutoValue
public abstract class TargetInfoLogMessage extends LogMessage implements Serializable {
  private static final long serialVersionUID = 1L;

  /** Creates a new log message containing the given benchmark model and device properties. */
  public static TargetInfoLogMessage create(
      BenchmarkClassModel model, Map<String, String> deviceProperties) {
    return new AutoValue_TargetInfoLogMessage(model, ImmutableMap.copyOf(deviceProperties));
  }

  /** Returns the benchmark class model. */
  public abstract BenchmarkClassModel model();

  /**
   * Returns the properties of the target device that will be used to populate the {@code Host}
   * associated with this target. The data included here will be available in the webapp for viewing
   * and comparison (when viewing data taken from multiple devices, such as when comparing multiple
   * runs).
   */
  public abstract ImmutableMap<String, String> deviceProperties();

  @Override
  public void accept(LogMessageVisitor visitor) {
    visitor.visit(this);
  }
}
