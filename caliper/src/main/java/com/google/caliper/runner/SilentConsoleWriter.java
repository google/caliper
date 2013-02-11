/*
 * Copyright (C) 2011 Google Inc.
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

import com.google.caliper.util.ShortDuration;

// This might actually be easier as a proxy so we don't have to keep updating it.
public class SilentConsoleWriter implements ConsoleWriter {
  @Override public void flush() {}
  @Override public void print(String s) {}
  @Override public void println(String s) {}
  @Override public void printf(String format, Object... args) {}
  @Override public void describe(ExperimentSelector selection) {}
  @Override public void beforeDryRun(int count) {}
  @Override public void beforeRun(int trials, int scenarioCount, ShortDuration estimate) { }
  @Override public void afterRun(ShortDuration elapsed) { }
  @Override public void skippedExperiments(int nSkipped) { }
}
