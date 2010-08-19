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

import com.google.caliper.UserException.DisplayUsageException;
import com.google.caliper.UserException.ExceptionFromUserCodeException;
import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.common.collect.ObjectArrays;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Map.Entry;
import java.util.TimeZone;

/**
 * Creates, executes and reports benchmark runs.
 */
public final class Runner {

  private static final FileFilter xmlFilter = new FileFilter() {
    @Override public boolean accept(File file) {
      return file.getName().endsWith(".xml");
    }
  };

  private static final String FILE_NAME_DATE_FORMAT = "yyyy-MM-dd'T'HH-mm-ssZ";
  private static final String LOG_DATE_FORMAT = "yyyy-MM-dd'T'HH-mm-ss.SSSZ";

  /** Command line arguments to the process */
  private Arguments arguments;
  private ScenarioSelection scenarioSelection;

  private String createFileName(Result result) {
    String timestamp = createTimestamp();
    return String.format("%s.%s.xml", result.getRun().getBenchmarkName(), timestamp);
  }

  private String createTimestamp() {
    SimpleDateFormat dateFormat = new SimpleDateFormat(FILE_NAME_DATE_FORMAT);
    dateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
    dateFormat.setLenient(true);
    return dateFormat.format(new Date());
  }

  public void run(String... args) {
    this.arguments = Arguments.parse(args);
    File xmlUploadFile = arguments.getXmlUploadFile();
    if (xmlUploadFile != null) {
      uploadXmlFileOrDir(xmlUploadFile);
      return;
    }
    this.scenarioSelection = new ScenarioSelection(arguments);
    Result result = runOutOfProcess();
    new ConsoleReport(result.getRun(), arguments).displayResults();
    boolean saveXmlLocally = arguments.getXmlSaveFile() != null;
    try {
      postResults(result);
    } catch (Exception e) {
      System.out.println();
      System.out.println(e);
      saveXmlLocally = true;
    }

    if (saveXmlLocally) {
      saveResultsToXml(result);
    }
  }

  private void uploadXmlFileOrDir(File xmlUploadFile) {
    try {
      if (xmlUploadFile.isDirectory()) {
        for (File xmlFile : xmlUploadFile.listFiles(xmlFilter)) {
          uploadXml(xmlFile);
        }
      } else {
        uploadXml(xmlUploadFile);
      }
    } catch (Exception e) {
      throw new RuntimeException("uploading XML file failed", e);
    }
  }

  private void uploadXml(File xmlUploadFile) throws IOException {
    System.out.println();
    System.out.println("Uploading " + xmlUploadFile.getCanonicalPath());
    Result result = Xml.resultFromXml(new FileInputStream(xmlUploadFile));
    postResults(result);
  }

  private void saveResultsToXml(Result result) {
    File xmlSaveFile = arguments.getXmlSaveFile();
    File destinationFile;
    if (xmlSaveFile == null) {
      File dir = new File("./caliper-results");
      dir.mkdirs();
      destinationFile = new File(dir, createFileName(result));
    } else if (xmlSaveFile.exists() && xmlSaveFile.isDirectory()) {
      destinationFile = new File(xmlSaveFile, createFileName(result));
    } else {
      // assume this is a file
      File parent = xmlSaveFile.getParentFile();
      if (parent != null) {
        parent.mkdirs();
      }
      destinationFile = xmlSaveFile;
    }
    try {
      System.out.println();
      System.out.println("Writing XML result to " + destinationFile.getCanonicalPath());
      FileOutputStream fileOutputStream = new FileOutputStream(destinationFile.getCanonicalPath());
      Xml.resultToXml(result, fileOutputStream);
      fileOutputStream.close();
    } catch (Exception e) {
      System.out.println(e);
      System.out.println("Failed to write XML results to file, writing to standard out instead:");
      Xml.resultToXml(result, System.out);
      System.out.flush();
    }
  }

  private void postResults(Result result) {
    CaliperRc caliperrc = CaliperRc.INSTANCE;
    String postUrl = caliperrc.getPostUrl();
    String apiKey = caliperrc.getApiKey();
    if (postUrl == null || apiKey == null) {
      // TODO: probably nicer to show a message if only one is null
      return;
    }

    try {
      URL url = new URL(postUrl + apiKey + "/" + result.getRun().getBenchmarkName());
      HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
      urlConnection.setDoOutput(true);
      Xml.resultToXml(result, urlConnection.getOutputStream());
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
      throw new RuntimeException("Posting to " + postUrl + " failed.", e);
    }
  }

  private MeasurementSetMeta executeForked(Scenario scenario) {
    String classPath = System.getProperty("java.class.path");
    if (classPath == null || classPath.length() == 0) {
      throw new IllegalStateException("java.class.path is undefined in " + System.getProperties());
    }

    ProcessBuilder builder = new ProcessBuilder();
    List<String> command = builder.command();
    List<String> vmList = Arrays.asList(scenario.getVariables().get(Scenario.VM_KEY).split("\\s+"));
    Vm vm = null;
    if (!vmList.isEmpty()) {
      if (vmList.get(0).endsWith("dalvikvm")) {
        vm = new DalvikVm();
      } else if (vmList.get(0).endsWith("java")) {
        vm = new StandardVm();
      }
    }
    if (vm == null) {
      vm = new UnknownVm();
    }
    command.addAll(vmList);
    command.add("-cp");
    command.add(classPath);
    command.add("-verbose:gc");
    for (String option : vm.getVmSpecificOptions()) {
      command.add(option);
    }
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
    Scenario normalizedScenario = null;
    try {
      builder.redirectErrorStream(true);
      builder.directory(new File(System.getProperty("user.dir")));

      vm.init();
      reader = vm.getLogReader(builder.start());
      LogParser logParser = vm.getLogProcessor();

      List<String> outputLines = Lists.newArrayList();

      SimpleDateFormat dateFormat = new SimpleDateFormat(LOG_DATE_FORMAT);
      dateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
      dateFormat.setLenient(true);

      String line;
      StringBuilder scenarioEventLog = new StringBuilder();
      while ((line = reader.readLine()) != null) {
        logParser.readLine(line);

        if (normalizedScenario == null) {
          if (logParser.getScenario() != null) {
            normalizedScenario = logParser.getScenario();
            System.out.print(normalizedScenario);
            System.out.flush();
          }
        }

        if (logParser.logLine()) {
          String logEntry = "[" + dateFormat.format(new Date()) + "] "
              + logParser.lineToLog() + "\n";
          scenarioEventLog.append(logEntry);
        }

        if (logParser.displayLine()) {
          outputLines.add(logParser.lineToDisplay());
        }

        if (logParser.isLogDone()) {
          break;
        }
      }
      vm.cleanup();

      MeasurementSet measurementSet = logParser.getMeasurementSet();
      if (measurementSet != null && normalizedScenario != null) {
        return new MeasurementSetMeta(measurementSet, normalizedScenario,
            scenarioEventLog.toString());
      }

      String message = "Failed to execute " + Joiner.on(" ").join(command);
      System.err.println();
      System.err.println("  " + message);
      for (String outputLine : outputLines) {
        System.err.println("  " + outputLine);
      }
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

  private Result runOutOfProcess() {
    Date executedDate = new Date();
    ImmutableMap.Builder<Scenario, MeasurementSetMeta> resultsBuilder = ImmutableMap.builder();

    try {
      List<Scenario> scenarios = scenarioSelection.select();

      int i = 0;
      for (Scenario scenario : scenarios) {
        beforeMeasurement(i++, scenarios.size());
        MeasurementSetMeta nanosPerTrial = executeForked(scenario);
        afterMeasurement(nanosPerTrial.getMeasurementSet());
        // use normalized scenario provided by the subprocess
        resultsBuilder.put(nanosPerTrial.getScenario(), nanosPerTrial);
      }
      System.out.println();

      Environment environment = new EnvironmentGetter().getEnvironmentSnapshot();
      return new Result(
          new Run(resultsBuilder.build(), arguments.getSuiteClassName(), executedDate),
          environment);
    } catch (Exception e) {
      throw new ExceptionFromUserCodeException(e);
    }
  }

  private void beforeMeasurement(int index, int total) {
    double percentDone = (double) index / total;
    System.out.printf("%2.0f%% ", percentDone * 100);
  }

  private void afterMeasurement(MeasurementSet measurementSet) {
    String unit =
        ConsoleReport.UNIT_ORDERING.min(measurementSet.getUnitNames().entrySet()).getKey();
    System.out.printf(" %.2f%s; \u03C3=%.2f%s @ %d trials%n", measurementSet.medianUnits(), unit,
        measurementSet.standardDeviationUnits(), unit, measurementSet.getMeasurements().size());
  }

  public static void main(String... args) {
    try {
      new Runner().run(args);
      System.exit(0); // user code may have leave non-daemon threads behind!
    } catch (DisplayUsageException e) {
      e.display();
      System.exit(0);
    } catch (UserException e) {
      e.display();
      System.exit(1);
    }
  }

  public static void main(Class<? extends Benchmark> suite, String... args) {
    main(ObjectArrays.concat(args, suite.getName()));
  }
}
