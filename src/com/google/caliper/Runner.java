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

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.LinkedHashMultimap;
import com.google.common.collect.Multimap;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.*;

/**
 * Creates, executes and reports benchmark runs.
 */
public final class Runner {

  private String suiteClassName;
  private Benchmark suite;

  /** Effective parameters to run in the benchmark. */
  private Multimap<String, String> parameters = LinkedHashMultimap.create();

  /** JVMs to run in the benchmark */
  private Set<String> userVms = new LinkedHashSet<String>();

  /**
   * Parameter values specified by the user on the command line. Parameters with
   * no value in this multimap will get their values from the benchmark suite.
   */
  private Multimap<String, String> userParameters = LinkedHashMultimap.create();

  /**
   * True if each benchmark should run in process.
   */
  private boolean inProcess;

  private long warmupMillis = 5000;
  private long runMillis = 5000;

  /**
   * Sets the named parameter to the specified value. This value will replace
   * the benchmark suite's default values for the parameter. Multiple calls to
   * this method will cause benchmarks for each value to be run.
   */
  void setParameter(String name, String value) {
    userParameters.put(name, value);
  }

  private void prepareSuite() {
    try {
      @SuppressWarnings("unchecked") // guarded by the if statement that follows
      Class<? extends Benchmark> suiteClass
          = (Class<? extends Benchmark>) Class.forName(suiteClassName);
      if (!Benchmark.class.isAssignableFrom(suiteClass)) {
        throw new ConfigurationException(suiteClass + " is not a benchmark suite.");
      }

      Constructor<? extends Benchmark> constructor = suiteClass.getDeclaredConstructor();
      suite = constructor.newInstance();
    } catch (InvocationTargetException e) {
      throw new ExecutionException(e.getCause());
    } catch (Exception e) {
      throw new ConfigurationException(e);
    }
  }

  private void prepareParameters() {
    for (String key : suite.parameterNames()) {
      // first check if the user has specified values
      Collection<String> userValues = userParameters.get(key);
      if (!userValues.isEmpty()) {
        parameters.putAll(key, userValues);
        // TODO: type convert 'em to validate?

      } else { // otherwise use the default values from the suite
        Set<String> values = suite.parameterValues(key);
        if (values.isEmpty()) {
          throw new ConfigurationException(key + " has no values");
        }
        parameters.putAll(key, values);
      }
    }
  }

  private ImmutableSet<String> defaultVms() {
    return "Dalvik".equals(System.getProperty("java.vm.name"))
        ? ImmutableSet.of("dalvikvm")
        : ImmutableSet.of("java");
  }

  /**
   * Returns a complete set of runs with every combination of values and
   * benchmark classes.
   */
  private List<Run> createRuns() throws Exception {
    List<RunBuilder> builders = new ArrayList<RunBuilder>();

    // create runs for each VMs
    Set<String> vms = userVms.isEmpty()
        ? defaultVms()
        : userVms;
    for (String vm : vms) {
      RunBuilder runBuilder = new RunBuilder();
      runBuilder.vm = vm;
      builders.add(runBuilder);
    }

    for (Map.Entry<String, Collection<String>> parameter : parameters.asMap().entrySet()) {
      Iterator<String> values = parameter.getValue().iterator();
      if (!values.hasNext()) {
        throw new ConfigurationException("Not enough values for " + parameter);
      }

      String key = parameter.getKey();

      String firstValue = values.next();
      for (RunBuilder builder : builders) {
        builder.parameters.put(key, firstValue);
      }

      // multiply the size of the specs by the number of alternate values
      int size = builders.size();
      while (values.hasNext()) {
        String alternate = values.next();
        for (int s = 0; s < size; s++) {
          RunBuilder copy = builders.get(s).copy();
          copy.parameters.put(key, alternate);
          builders.add(copy);
        }
      }
    }

    List<Run> result = new ArrayList<Run>();
    for (RunBuilder builder : builders) {
      result.add(builder.build());
    }

    return result;
  }

  static class RunBuilder {
    Map<String, String> parameters = new LinkedHashMap<String, String>();
    String vm;

    RunBuilder copy() {
      RunBuilder result = new RunBuilder();
      result.parameters.putAll(parameters);
      result.vm = vm;
      return result;
    }

    public Run build() {
      return new Run(parameters, vm);
    }
  }

  private double executeForked(Run run) {
    ProcessBuilder builder = new ProcessBuilder();
    List<String> command = builder.command();
    command.addAll(Arrays.asList(run.getVm().split("\\s+")));
    command.add("-cp");
    command.add(System.getProperty("java.class.path"));
    command.add(Runner.class.getName());
    command.add("--warmupMillis");
    command.add(String.valueOf(warmupMillis));
    command.add("--runMillis");
    command.add(String.valueOf(runMillis));
    command.add("--inProcess");
    for (Map.Entry<String, String> entry : run.getParameters().entrySet()) {
      command.add("-D" + entry.getKey() + "=" + entry.getValue());
    }
    command.add(suiteClassName);

    BufferedReader reader = null;
    try {
      builder.redirectErrorStream(true);
      builder.directory(new File(System.getProperty("user.dir")));
      Process process = builder.start();

      reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
      String firstLine = reader.readLine();
      Double nanosPerTrial = null;
      try {
        nanosPerTrial = Double.valueOf(firstLine);
      } catch (NumberFormatException e) {
      }

      String anotherLine = reader.readLine();
      if (nanosPerTrial != null && anotherLine == null) {
        return nanosPerTrial;
      }

      String message = "Failed to execute " + command;
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

  private Result runOutOfProcess() {
    ImmutableMap.Builder<Run, Double> resultsBuilder = ImmutableMap.builder();

    try {
      List<Run> runs = createRuns();
      int i = 0;
      for (Run run : runs) {
        beforeRun(i++, runs.size(), run);
        double nanosPerTrial = executeForked(run);
        afterRun(nanosPerTrial);
        resultsBuilder.put(run, nanosPerTrial);
      }

      // blat out our progress bar
      System.out.print("\r");
      for (int j = 0; j < 80; j++) {
        System.out.print(" ");
      }
      System.out.print("\r");

      return new Result(resultsBuilder.build());
    } catch (Exception e) {
      throw new ExecutionException(e);
    }
  }

  private void beforeRun(int index, int total, Run run) {
    double percentDone = (double) index / total;
    int runStringLength = 63; // so the total line length is 80
    String runString = String.valueOf(run);
    if (runString.length() > runStringLength) {
      runString = runString.substring(0, runStringLength);
    }
    System.out.printf("\r%2.0f%% %-" + runStringLength + "s",
        percentDone * 100, runString);
  }

  private void afterRun(double nanosPerTrial) {
    System.out.printf(" %10.0fns", nanosPerTrial);
  }

  private void runInProcess() {
    try {
      Caliper caliper = new Caliper(warmupMillis, runMillis);

      for (Run run : createRuns()) {
        double result;
        TimedRunnable timedRunnable = suite.createBenchmark(run.getParameters());
        double warmupNanosPerTrial = caliper.warmUp(timedRunnable);
        result = caliper.run(timedRunnable, warmupNanosPerTrial);
        double nanosPerTrial = result;
        System.out.println(nanosPerTrial);
      }
    } catch (Exception e) {
      throw new ExecutionException(e);
    }
  }

  private boolean parseArgs(String[] args) throws Exception {
    for (int i = 0; i < args.length; i++) {
      if ("--help".equals(args[i])) {
        return false;

      } else if ("--inProcess".equals(args[i])) {
          inProcess = true;

      } else if (args[i].startsWith("-D")) {
        int equalsSign = args[i].indexOf('=');
        if (equalsSign == -1) {
          System.out.println("Malformed parameter " + args[i]);
          return false;
        }
        String name = args[i].substring(2, equalsSign);
        String value = args[i].substring(equalsSign + 1);
        setParameter(name, value);

      } else if ("--warmupMillis".equals(args[i])) {
        warmupMillis = Long.parseLong(args[++i]);

      } else if ("--runMillis".equals(args[i])) {
        runMillis = Long.parseLong(args[++i]);

      } else if ("--vm".equals(args[i])) {
        userVms.add(args[++i]);

      } else if (args[i].startsWith("-")) {
        System.out.println("Unrecognized option: " + args[i]);
        return false;

      } else {
        if (suiteClassName != null) {
          System.out.println("Too many benchmark classes!");
          return false;
        }
        suiteClassName = args[i];
      }
    }

    if (inProcess && !userVms.isEmpty()) {
      System.out.println("Cannot customize VM when running in process");
      return false;
    }

    if (suiteClassName == null) {
      System.out.println("No benchmark class provided.");
      return false;
    }

    return true;
  }

  private void printUsage() {
    System.out.println("Usage: Runner [OPTIONS...] <benchmark>");
    System.out.println();
    System.out.println("  <benchmark>: a benchmark class or suite");
    System.out.println();
    System.out.println("OPTIONS");
    System.out.println();
    System.out.println("  --D<param>=<value>: fix a benchmark parameter to a given value.");
    System.out.println("        When multiple values for the same parameter are given (via");
    System.out.println("        multiple --Dx=y args), all supplied values are used.");
    System.out.println();
    System.out.println("  --inProcess: run the benchmark in the same JVM rather than spawning");
    System.out.println("        another with the same classpath. By default each benchmark is");
    System.out.println("        run in a separate VM");
    System.out.println();
    System.out.println("  --warmupMillis <millis>: duration to warmup each benchmark");
    System.out.println();
    System.out.println("  --runMillis <millis>: duration to execute each benchmark");
    System.out.println();
    System.out.println("  --vm <vm>: executable to test benchmark on");

    // adding new options? don't forget to update executeForked()
  }

  public static void main(String... args) throws Exception { // TODO: cleaner error reporting
    Runner runner = new Runner();
    if (!runner.parseArgs(args)) {
      runner.printUsage();
      return;
    }

    runner.prepareSuite();
    runner.prepareParameters();
    if (runner.inProcess) {
      runner.runInProcess();
      return;
    }

    Result result = runner.runOutOfProcess();
    new ConsoleReport(result).displayResults();
  }

  public static void main(Class<? extends Benchmark> suite, String... args) throws Exception {
    String[] argsWithSuiteName = new String[args.length + 1];
    System.arraycopy(args, 0, argsWithSuiteName, 0, args.length);
    argsWithSuiteName[args.length] = suite.getName();
    main(argsWithSuiteName);
  }
}
