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
import com.google.common.base.Function;
import com.google.common.collect.FluentIterable;

import java.io.PrintWriter;

final class DefaultConsoleWriter implements ConsoleWriter {
  private final PrintWriter writer;

  DefaultConsoleWriter(PrintWriter writer) {
    this.writer = writer;
  }

  @Override public void flush() {
    writer.flush();
  }

  @Override public void print(String s) {
    writer.print(s);
  }

  @Override public void println(String s) {
    writer.println(s);
  }

  @Override public void printf(String format, Object... args) {
    writer.printf(format, args);
  }

  @Override public void describe(ExperimentSelector selector) {
    writer.println("Experiment selection: ");
    writer.println("  Instruments:   " + FluentIterable.from(selector.instruments())
        .transform(new Function<Instrument, String>() {
              @Override public String apply(Instrument instrument) {
                return instrument.name();
              }
            }));
    writer.println("  User parameters:   " + selector.userParameters());
    writer.println("  Virtual machines:  " + FluentIterable.from(selector.vms())
        .transform(
            new Function<VirtualMachine, String>() {
              @Override public String apply(VirtualMachine vm) {
                return vm.name;
              }
            }));
    writer.println("  Selection type:    " + selector.selectionType());
    writer.println();
  }

  @Override public void beforeDryRun(int experimentCount) {
    writer.format("This selection yields %s experiments.%n", experimentCount);
  }

  @Override public void beforeRun(int trials, int experimentCount, ShortDuration estimate) {
    writer.format("Measuring %s trials each of %s experiments. ", trials, experimentCount);
    if (estimate.equals(ShortDuration.zero())) {
      writer.println("(Cannot estimate runtime.)");
    } else {
      writer.format("Estimated runtime: %s.%n", estimate);
    }
  }

  @Override public void afterRun(ShortDuration elapsed) {
    writer.format("Execution complete: %s.%n", elapsed);
  }

  @Override public void skippedExperiments(int nSkipped) {
    writer.format("%d experiments were skipped.%n", nSkipped);
  }
}
