/*
 * Copyright (C) 2011 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */

package com.google.caliper.runner;

import java.io.PrintWriter;

/** Future home of the caliper runner. */
public final class CaliperRun implements Runnable {
  private final CaliperOptions options;
  private final PrintWriter writer;

  /**
   * Standard constructor.
   */
  public CaliperRun(CaliperOptions options, PrintWriter writer) {
    this.options = options;
    this.writer = writer;
  }

  public void run() {
    // this is where we'll do stuff
  }
}
