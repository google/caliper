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
import com.google.common.io.Closeables;
import com.google.gson.JsonObject;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintStream;
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

  private static final FileFilter UPLOAD_FILE_FILTER = new FileFilter() {
    @Override public boolean accept(File file) {
      return file.getName().endsWith(".xml") || file.getName().endsWith(".json");
    }
  };

  private static final String FILE_NAME_DATE_FORMAT = "yyyy-MM-dd'T'HH-mm-ssZ";

  /** Command line arguments to the process */
  private Arguments arguments;
  private ScenarioSelection scenarioSelection;

  private String createFileName(Result result) {
    String timestamp = createTimestamp();
    return String.format("%s.%s.json", result.getRun().getBenchmarkName(), timestamp);
  }

  private String createTimestamp() {
    SimpleDateFormat dateFormat = new SimpleDateFormat(FILE_NAME_DATE_FORMAT);
    dateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
    dateFormat.setLenient(true);
    return dateFormat.format(new Date());
  }

  public void run(String... args) {
    this.arguments = Arguments.parse(args);
    File resultsUploadFile = arguments.getUploadResultsFile();
    if (resultsUploadFile != null) {
      uploadResultsFileOrDir(resultsUploadFile);
      return;
    }
    this.scenarioSelection = new ScenarioSelection(arguments);
    Result result = runOutOfProcess();
    new ConsoleReport(result.getRun(), arguments).displayResults();
    boolean saveResultsLocally = arguments.getSaveResultsFile() != null;
    try {
      postResults(result);
    } catch (Exception e) {
      System.out.println();
      System.out.println(e);
      saveResultsLocally = true;
    }

    if (saveResultsLocally) {
      saveResults(result);
    }
  }

  void uploadResultsFileOrDir(File resultsFileOrDir) {
    try {
      if (resultsFileOrDir.isDirectory()) {
        for (File resultsFile : resultsFileOrDir.listFiles(UPLOAD_FILE_FILTER)) {
          uploadResults(resultsFile);
        }
      } else {
        uploadResults(resultsFileOrDir);
      }
    } catch (Exception e) {
      throw new RuntimeException("uploading XML file failed", e);
    }
  }

  private void uploadResults(File resultsUploadFile) throws IOException {
    System.out.println();
    System.out.println("Uploading " + resultsUploadFile.getCanonicalPath());
    InputStream inputStream = new FileInputStream(resultsUploadFile);
    try {
      Result result = new ResultsReader().getResult(inputStream);
      postResults(result);
    } finally {
      inputStream.close();
    }
  }

  private void saveResults(Result result) {
    File resultsFile = arguments.getSaveResultsFile();
    File destinationFile;
    if (resultsFile == null) {
      File dir = new File("./caliper-results");
      dir.mkdirs();
      destinationFile = new File(dir, createFileName(result));
    } else if (resultsFile.exists() && resultsFile.isDirectory()) {
      destinationFile = new File(resultsFile, createFileName(result));
    } else {
      // assume this is a file
      File parent = resultsFile.getParentFile();
      if (parent != null) {
        parent.mkdirs();
      }
      destinationFile = resultsFile;
    }

    PrintStream filePrintStream;
    try {
      filePrintStream = new PrintStream(new FileOutputStream(destinationFile));
    } catch (FileNotFoundException e) {
      throw new RuntimeException("can't open " + destinationFile, e);
    }
    String resultJson = Json.getGsonInstance().toJson(result);
    try {
      System.out.println();
      System.out.println("Writing results to " + destinationFile.getCanonicalPath());
      filePrintStream.print(resultJson);
    } catch (Exception e) {
      System.out.println(e);
      System.out.println("Failed to write results to file, writing to standard out instead:");
      System.out.println(resultJson);
      System.out.flush();
    } finally {
      filePrintStream.close();
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
      String resultJson = Json.getGsonInstance().toJson(result);
      urlConnection.getOutputStream().write(resultJson.getBytes());
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

  private ScenarioResult runScenario(Scenario scenario) {
    MeasurementResult timeMeasurementResult = measure(scenario, MeasurementType.TIME);
    MeasurementSet allocationMeasurements = null;
    String allocationEventLog = null;
    MeasurementSet memoryMeasurements = null;
    String memoryEventLog = null;
    if (arguments.getMeasureMemory()) {
      MeasurementResult allocationsMeasurementResult =
          measure(scenario, MeasurementType.INSTANCE);
      allocationMeasurements = allocationsMeasurementResult.getMeasurements();
      allocationEventLog = allocationsMeasurementResult.getEventLog();
      MeasurementResult memoryMeasurementResult =
          measure(scenario, MeasurementType.MEMORY);
      memoryMeasurements = memoryMeasurementResult.getMeasurements();
      memoryEventLog = memoryMeasurementResult.getEventLog();
    }

    return new ScenarioResult(timeMeasurementResult.getMeasurements(),
        timeMeasurementResult.getEventLog(),
        allocationMeasurements, allocationEventLog,
        memoryMeasurements, memoryEventLog);
  }

  private class MeasurementResult {
    private final MeasurementSet measurements;
    private final String eventLog;

    MeasurementResult(MeasurementSet measurements, String eventLog) {
      this.measurements = measurements;
      this.eventLog = eventLog;
    }

    public MeasurementSet getMeasurements() {
      return measurements;
    }

    public String getEventLog() {
      return eventLog;
    }
  }

  private MeasurementResult measure(Scenario scenario, MeasurementType type) {
    Vm vm = new VmFactory().createVm(scenario);
    // this must be done before starting the forked process on certain VMs
    List<String> command = createCommand(scenario, vm, type);
    Process timeProcess = startForkedProcess(command);

    MeasurementSet measurementSet = null;
    StringBuilder eventLog = new StringBuilder();
    InterleavedReader reader = null;
    try {
      reader = new InterleavedReader(arguments.getMarker(),
          new InputStreamReader(timeProcess.getInputStream()));
      Object o;
      while ((o = reader.read()) != null) {
        if (o instanceof String) {
          eventLog.append(o);
        } else if (measurementSet == null) {
          JsonObject jsonObject = (JsonObject) o;
          measurementSet = Json.measurementSetFromJson(jsonObject);
        } else {
          throw new RuntimeException("Unexpected value: " + o);
        }
      }
    } catch (IOException e) {
      throw new RuntimeException(e);
    } finally {
      Closeables.closeQuietly(reader);
    }

    if (measurementSet == null) {
      String message = "Failed to execute " + Joiner.on(" ").join(command);
      System.err.println("  " + message);
      System.err.println(eventLog.toString());
      throw new ConfigurationException(message);
    }

    return new MeasurementResult(measurementSet, eventLog.toString());
  }

  private List<String> createCommand(Scenario scenario, Vm vm, MeasurementType type) {
    String classPath = System.getProperty("java.class.path");
    if (classPath == null || classPath.length() == 0) {
      throw new IllegalStateException("java.class.path is undefined in " + System.getProperties());
    }

    List<String> command = Lists.newArrayList();
    List<String> vmList = Arrays.asList(scenario.getVariables().get(Scenario.VM_KEY).split("\\s+"));
    command.addAll(vmList);
    command.add("-cp");
    command.add(classPath);
    command.add("-verbose:gc");
    if (type == MeasurementType.INSTANCE || type == MeasurementType.MEMORY) {
      String allocationJarFile = System.getenv("ALLOCATION_JAR");
      command.add("-javaagent:" + allocationJarFile);
    }
    for (String option : vm.getVmSpecificOptions(type)) {
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
    command.add("--measurementType");
    command.add(type.toString());
    command.add("--marker");
    command.add(arguments.getMarker());
    command.add(arguments.getSuiteClassName());
    return command;
  }

  private Process startForkedProcess(List<String> command) {
    ProcessBuilder builder = new ProcessBuilder();
    builder.command(command);
    builder.redirectErrorStream(true);
    builder.directory(new File(System.getProperty("user.dir")));
    try {
      return builder.start();
    } catch (IOException e) {
      throw new RuntimeException("failed to start subprocess", e);
    }
  }

  private Result runOutOfProcess() {
    Date executedDate = new Date();
    ImmutableMap.Builder<Scenario, ScenarioResult> resultsBuilder = ImmutableMap.builder();

    try {
      List<Scenario> scenarios = scenarioSelection.select();

      int i = 0;
      for (Scenario scenario : scenarios) {
        beforeMeasurement(i++, scenarios.size(), scenario);
        ScenarioResult scenarioResult = runScenario(scenario);
        afterMeasurement(arguments.getMeasureMemory(), scenarioResult);
        resultsBuilder.put(scenario, scenarioResult);
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

  private void beforeMeasurement(int index, int total, Scenario scenario) {
    double percentDone = (double) index / total;
    System.out.printf("%2.0f%% %s", percentDone * 100, scenario);
  }

  private void afterMeasurement(boolean memoryMeasured, ScenarioResult scenarioResult) {
    String memoryMeasurements = "";
    if (memoryMeasured) {
      MeasurementSet instanceMeasurementSet =
          scenarioResult.getMeasurementSet(MeasurementType.INSTANCE);
      String instanceUnit =
        ConsoleReport.UNIT_ORDERING.min(instanceMeasurementSet.getUnitNames().entrySet()).getKey();
      MeasurementSet memoryMeasurementSet = scenarioResult.getMeasurementSet(MeasurementType.MEMORY);
      String memoryUnit =
        ConsoleReport.UNIT_ORDERING.min(memoryMeasurementSet.getUnitNames().entrySet()).getKey();
      memoryMeasurements = String.format(", allocated %s%s for a total of %s%s",
          Math.round(instanceMeasurementSet.medianUnits()), instanceUnit,
          Math.round(memoryMeasurementSet.medianUnits()), memoryUnit);
    }

    MeasurementSet timeMeasurementSet = scenarioResult.getMeasurementSet(MeasurementType.TIME);
    String unit =
        ConsoleReport.UNIT_ORDERING.min(timeMeasurementSet.getUnitNames().entrySet()).getKey();
    System.out.printf(" %.2f %s; \u03C3=%.2f %s @ %d trials%s%n", timeMeasurementSet.medianUnits(),
        unit, timeMeasurementSet.standardDeviationUnits(), unit,
        timeMeasurementSet.getMeasurements().size(), memoryMeasurements);
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
