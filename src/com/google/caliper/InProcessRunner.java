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

import com.google.caliper.UserException.CantCustomizeInProcessVmException;
import com.google.caliper.UserException.ExceptionFromUserCodeException;
import com.google.common.base.Supplier;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;

/**
 * Executes a benchmark in the current VM.
 */
final class InProcessRunner {

  public void run(String... args) {
    Arguments arguments = Arguments.parse(args);

    if (!arguments.getUserVms().isEmpty()) {
      throw new CantCustomizeInProcessVmException();
    }

    final ScenarioSelection scenarioSelection = new ScenarioSelection(arguments);

    PrintStream outStream = System.out;
    PrintStream errStream = System.err;
    System.setOut(nullPrintStream());
    System.setErr(nullPrintStream());
    try {
      Caliper caliper = new Caliper(arguments.getWarmupMillis(), arguments.getRunMillis());

      for (final Scenario scenario : scenarioSelection.select()) {
        Supplier<TimedRunnable> supplier = new Supplier<TimedRunnable>() {
          public TimedRunnable get() {
            return scenarioSelection.createBenchmark(scenario);
          }
        };

        double warmupNanosPerTrial = caliper.warmUp(supplier);
        MeasurementSet measurementSet = caliper.run(supplier, warmupNanosPerTrial);
        outStream.println(measurementSet);
      }
    } catch (Exception e) {
      throw new ExceptionFromUserCodeException(e);
    } finally {
      System.setOut(outStream);
      System.setErr(errStream);
    }
  }

  public static void main(String... args) {
    try {
      new InProcessRunner().run(args);
      System.exit(0); // user code may have leave non-daemon threads behind!
    } catch (UserException e) {
      e.display(); // TODO: send this to the host process
      System.exit(1);
    }
  }

  public PrintStream nullPrintStream() {
    return new PrintStream(new OutputStream() {
      public void write(int b) throws IOException {}
    });
  }
}
