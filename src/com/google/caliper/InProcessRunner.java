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
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.List;
import java.util.Map.Entry;
import java.util.Properties;

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
      Caliper caliper = new Caliper(arguments.getWarmupMillis(), arguments.getRunMillis(),
          outStream);

      log(outStream, LogConstants.SCENARIOS_STARTING);
      List<Scenario> scenarios = scenarioSelection.select();
      // We only expect one scenario right now - if we have more, something has gone wrong.
      // This matters for things like reading the measurements. This is only done once, so if
      // multiple scenarios are executed, they will be ignored!
      if (scenarios.size() != 1) {
        throw new IllegalArgumentException("Invalid arguments to subprocess. Expected exactly one "
            + "scenario but got " + scenarios.size());
      }
      for (Scenario scenario : scenarios) {
        final Scenario normalizedScenario = scenarioSelection.normalizeScenario(scenario);
        Supplier<ConfiguredBenchmark> supplier = new Supplier<ConfiguredBenchmark>() {
          @Override public ConfiguredBenchmark get() {
            return scenarioSelection.createBenchmark(normalizedScenario);
          }
        };

        ByteArrayOutputStream scenarioXml = new ByteArrayOutputStream();
        getScenarioProperties(normalizedScenario).storeToXML(scenarioXml, "");
        // output xml on a single line so it's easier to parse on the other side.
        outStream.println(LogConstants.SCENARIO_XML_PREFIX
            + scenarioXml.toString().replaceAll("\r\n|\r|\n", ""));

        double warmupNanosPerTrial = caliper.warmUp(supplier);
        log(outStream, LogConstants.STARTING_SCENARIO_PREFIX + normalizedScenario);
        MeasurementSet measurementSet = caliper.run(supplier, warmupNanosPerTrial);
        log(outStream, LogConstants.MEASUREMENT_PREFIX
            + Json.measurementSetToJson(measurementSet));
      }
      log(outStream, LogConstants.SCENARIOS_FINISHED);
    } catch (UserException e) {
      throw e;
    } catch (Exception e) {
      throw new ExceptionFromUserCodeException(e);
    } finally {
      System.setOut(outStream);
      System.setErr(errStream);
    }
  }

  private Properties getScenarioProperties(Scenario scenario) {
    Properties properties = new Properties();
    for (Entry<String, String> entry : scenario.getVariables().entrySet()) {
      properties.setProperty(entry.getKey(), entry.getValue());
    }
    return properties;
  }

  private void log(PrintStream outStream, String message) {
    outStream.println(LogConstants.CALIPER_LOG_PREFIX + message);
  }

  public static void main(String... args) {
    try {
      new InProcessRunner().run(args);
      System.exit(0); // user code may have leave non-daemon threads behind!
    } catch (UserException e) {
      e.display(); // TODO: send this to the host process
      System.out.println(LogConstants.CALIPER_LOG_PREFIX + LogConstants.SCENARIOS_FINISHED);
      System.exit(1);
    }
  }

  public PrintStream nullPrintStream() {
    return new PrintStream(new OutputStream() {
      public void write(int b) throws IOException {}
    });
  }
}
