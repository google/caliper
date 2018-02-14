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
import java.io.Serializable;

/**
 * A log message containing a {@link BenchmarkClassModel}.
 *
 * @author Colin Decker
 */
@AutoValue
public abstract class BenchmarkModelLogMessage extends LogMessage implements Serializable {
  private static final long serialVersionUID = 1L;

  /** Creates a new log message containing the given benchmark model. */
  public static BenchmarkModelLogMessage create(BenchmarkClassModel model) {
    return new AutoValue_BenchmarkModelLogMessage(model);
  }

  /** Returns the benchmark class model. */
  public abstract BenchmarkClassModel model();

  @Override
  public void accept(LogMessageVisitor visitor) {
    visitor.visit(this);
  }
}
