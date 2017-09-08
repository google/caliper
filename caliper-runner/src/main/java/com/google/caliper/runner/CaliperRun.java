/*
 * Copyright (C) 2012 Google Inc.
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

import com.google.caliper.core.InvalidBenchmarkException;

/**
 * A single Caliper benchmark run.
 *
 * <p>Unlike {@link CaliperRunner}, this only handles the actual run of a benchmark once various
 * setup (such as creating the model of the benchmark class) has been done.
 */
public interface CaliperRun {

  /*
   * TODO(cgdecker): Figure out a better way do differentiate between this and CaliperRunner.
   * Right now I think it's pretty unclear. Maybe this should be "BenchmarkRun"? Or maybe
   * CaliperRunner should be CaliperMain (but that would require other changes and also I think it's
   * helpful to make it clear what the "runner" is since that term is used a lot)? Or maybe I should
   * find a way to avoid this separation at all.
   */

  /** Runs the benchmark. */
  void run() throws InvalidBenchmarkException;
}
