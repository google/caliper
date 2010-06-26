/*
 * Copyright (C) 2009 Google Inc.
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

import com.google.caliper.UserException.ExceptionFromUserCodeException;
import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ObjectArrays;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Map.Entry;

/**
 * Creates, executes and reports benchmark runs.
 */
public final class Runner {

  /** Command line arguments to the process */
  private Arguments arguments;
  private ScenarioSelection scenarioSelection;

  public void run(String... args) {
    this.arguments = Arguments.parse(args);
    this.scenarioSelection = new ScenarioSelection(arguments);
    Run run = runOutOfProcess();
    new ConsoleReport(run, arguments).displayResults();
    postResults(run);
  }

  private void postResults(Run run) {
    CaliperRc caliperrc = CaliperRc.INSTANCE;
    String postUrl = caliperrc.getPostUrl();
    String apiKey = caliperrc.getApiKey();
    if (postUrl == null || apiKey == null) {
      // TODO: probably nicer to show a message if only one is null
      return;
    }

    try {
      URL url = new URL(postUrl + apiKey + "/" + run.getBenchmarkName());
      HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
      urlConnection.setDoOutput(true);
      Xml.runToXml(run, urlConnection.getOutputStream());
      if (urlConnection.getResponseCode() == 200) {
        System.out.println("");
        System.out.println("View current and previous benchmark results online:");
        BufferedReader in = new BufferedReader(
            new InputStreamReader(urlConnection.getInputStream()));
        System.out.println("  " + in.readLine());
        return;
      }

      System.out.println("Posting to " + postUrl + " failed: "
          + urlConnection.getResponseMessage());
      BufferedReader reader = new BufferedReader(
          new InputStreamReader(urlConnection.getInputStream()));
      String line;
      while ((line = reader.readLine()) != null) {
        System.out.println(line);
      }
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  private MeasurementSet executeForked(Scenario scenario) {
    ProcessBuilder builder = new ProcessBuilder();
    List<String> command = builder.command();
    command.addAll(Arrays.asList(scenario.getVariables().get(Scenario.VM_KEY).split("\\s+")));
    command.add("-cp");
    command.add(System.getProperty("java.class.path"));
    command.add(InProcessRunner.class.getName());
    command.add("--warmupMillis");
    command.add(String.valueOf(arguments.getWarmupMillis()));
    command.add("--runMillis");
    command.add(String.valueOf(arguments.getRunMillis()));
    for (Entry<String, String> entry : scenario.getParameters().entrySet()) {
      command.add("-D" + entry.getKey() + "=" + entry.getValue());
    }
    command.add(arguments.getSuiteClassName());

    BufferedReader reader = null;
    try {
      builder.redirectErrorStream(true);
      builder.directory(new File(System.getProperty("user.dir")));
      Process process = builder.start();

      reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
      String firstLine = reader.readLine();
      String anotherLine = reader.readLine();
      if (firstLine != null && anotherLine == null) {
        try {
          return MeasurementSet.valueOf(firstLine);
        } catch (IllegalArgumentException ignore) {
        }
      }

      String message = "Failed to execute " + Joiner.on(" ").join(command);
      System.err.println(message);
      System.err.println("  " + firstLine);
      do {
        System.err.println("  " + anotherLine);
      } while ((anotherLine = reader.readLine()) != null);
      throw new ConfigurationException(message);
    } catch (IOException e) {
      throw new ConfigurationException(e);
    } finally {
      if (reader != null) {
        try {
          reader.close();
        } catch (IOException ignored) {
        }
      }
    }
  }

  private Run runOutOfProcess() {
    Date executedDate = new Date();
    ImmutableMap.Builder<Scenario, MeasurementSet> resultsBuilder = ImmutableMap.builder();

    try {
      List<Scenario> scenarios = scenarioSelection.select();
      int i = 0;
      for (Scenario scenario : scenarios) {
        beforeMeasurement(i++, scenarios.size(), scenario);
        MeasurementSet nanosPerTrial = executeForked(scenario);
        afterMeasurement(nanosPerTrial);
        resultsBuilder.put(scenario, nanosPerTrial);
      }
      System.out.println();

      String apiKey = CaliperRc.INSTANCE.getApiKey();
      return new Run(resultsBuilder.build(), arguments.getSuiteClassName(), apiKey, executedDate);
    } catch (Exception e) {
      throw new ExceptionFromUserCodeException(e);
    }
  }

  private void beforeMeasurement(int index, int total, Scenario scenario) {
    double percentDone = (double) index / total;
    System.out.printf("%2.0f%% %s", percentDone * 100, scenario);
  }

  private void afterMeasurement(MeasurementSet measurementSet) {
    System.out.printf(" %.2fns; \u03C3=%.2fns @ %d trials%n", measurementSet.median(),
        measurementSet.standardDeviation(), measurementSet.getMeasurements().length);
  }

  public static void main(String... args) {
    try {
      new Runner().run(args);
    } catch (UserException e) {
      e.display();
      System.exit(1);
    }
  }

  public static void main(Class<? extends Benchmark> suite, String... args) {
    main(ObjectArrays.concat(args, suite.getName()));
  }
}
