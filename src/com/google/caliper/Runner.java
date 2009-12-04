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

import com.google.common.collect.Multimap;
import com.google.common.collect.LinkedHashMultimap;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.*;

/**
 * Creates, executes and reports benchmark runs.
 */
public final class Runner {

  private String suiteClassName;
  private BenchmarkSuite suite;

  /**
   * Parameter values specified by the user on the command line.
   */
  private Multimap<String, String> userParameters = LinkedHashMultimap.create();

  /**
   * Effective parameters to run in the benchmark.
   */
  private Multimap<String, String> parameters = LinkedHashMultimap.create();

  /**
   * Sets the named parameter to the specified value. This value will replace
   * the benchmark suite's default values for the parameter. Multiple calls to
   * this method will cause benchmarks for each value to be run.
   */
  public void setParameter(String name, String value) {
    userParameters.put(name, value);
  }

  private void prepareSuite() {
    try {
      @SuppressWarnings("unchecked") // guarded by the if statement that follows
      Class<? extends BenchmarkSuite> suiteClass
          = (Class<? extends BenchmarkSuite>) Class.forName(suiteClassName);
      if (!BenchmarkSuite.class.isAssignableFrom(suiteClass)) {
        throw new ConfigurationException(suiteClass + " is not a benchmark suite.");
      }

      Constructor<? extends BenchmarkSuite> constructor = suiteClass.getDeclaredConstructor();
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

  private void run() {
    List<Run> runs;
    try {
      runs = createRuns();
    } catch (Exception e) {
      throw new ExecutionException(e);
    }

    System.out.println(runs.size() + " runs...");
    for (Run run : runs) {
      execute(run);
    }
  }

  /**
   * Returns a complete set of runs with every combination of values and
   * benchmark classes.
   */
  private List<Run> createRuns() throws Exception {
    List<RunBuilder> builders = new ArrayList<RunBuilder>();
    for (Class<? extends Benchmark> benchmarkClass : suite.benchmarkClasses()) {
      RunBuilder builder = new RunBuilder();
      builder.benchmarkClass = benchmarkClass;
      builders.add(builder);
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
      int length = builders.size();
      while (values.hasNext()) {
        String alternate = values.next();
        for (int s = 0; s < length; s++) {
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
    Class<? extends Benchmark> benchmarkClass;
    Map<String, String> parameters = new LinkedHashMap<String, String>();

    RunBuilder copy() {
      RunBuilder result = new RunBuilder();
      result.benchmarkClass = benchmarkClass;
      result.parameters.putAll(parameters);
      return result;
    }

    public Run build() {
      return new Run(benchmarkClass, parameters);
    }
  }

  private void execute(Run run) {
    Benchmark benchmark = suite.createBenchmark(
        run.getBenchmarkClass(), run.getParameters());

    try {
      System.out.println(run);
      long start = System.nanoTime();
      benchmark.run(10000);
      long finish = System.nanoTime();
      long duration = finish - start;
      System.out.println(((duration + 500000) / 1000000) + "ms");
    } catch (Exception e) {
      throw new ExecutionException(e);
    }
  }

  private boolean parseArgs(String[] args) {
    for (int i = 0; i < args.length; i++) {
      if ("--help".equals(args[i])) {
        return false;

      } else if (args[i].startsWith("-D")) {
        int equalsSign = args[i].indexOf('=');
        if (equalsSign == -1) {
          System.out.println("Malformed parameter " + args[i]);
          return false;
        }
        String name = args[i].substring(2, equalsSign);
        String value = args[i].substring(equalsSign + 1);
        setParameter(name, value);

      } else if (args[i].startsWith("-")) {
        System.out.println("Unrecognized option: " + args[i]);

      } else {
        if (suiteClassName != null) {
          System.out.println("Too many benchmark classes!");
          return false;
        }

        suiteClassName = args[i];

      }
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
    System.out.println("  --D<param>=<value>: fix a benchmark parameter to a given value");
  }

  public static void main(String... args) {
    Runner runner = new Runner();
    if (!runner.parseArgs(args)) {
      runner.printUsage();
      return;
    }

    runner.prepareSuite();
    runner.prepareParameters();
    runner.run();
  }

  public static void main(Class<? extends BenchmarkSuite> suite, String... args) {
    String[] argsWithSuiteName = new String[args.length + 1];
    System.arraycopy(args, 0, argsWithSuiteName, 0, args.length);
    argsWithSuiteName[args.length] = suite.getName();
    main(argsWithSuiteName);
  }
}
