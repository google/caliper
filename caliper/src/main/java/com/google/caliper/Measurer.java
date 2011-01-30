/*
 * Copyright (C) 2010 Google Inc.
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

package com.google.caliper;

import com.google.common.base.Supplier;
import java.io.PrintStream;

abstract class Measurer {

  private PrintStream logStream = System.out;

  /**
   * Sets the stream used to log caliper events.
   */
  void setLogStream(PrintStream logStream) {
    this.logStream = logStream;
  }

  public abstract MeasurementSet run(Supplier<ConfiguredBenchmark> testSupplier) throws Exception;

  protected void prepareForTest() {
    System.gc();
    System.gc();
  }

  protected final void log(String message) {
    logStream.println(LogConstants.CALIPER_LOG_PREFIX + message);
  }
}
