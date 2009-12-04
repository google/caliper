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

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.Collection;

public final class Runner {

  private String suiteClassName;
  private BenchmarkSuite suite;

  private void prepare() {
    try {
      @SuppressWarnings("unchecked") // guarded by the if statement that follows
      Class<? extends BenchmarkSuite> suiteClass
          = (Class<? extends BenchmarkSuite>) Class.forName(suiteClassName);
      if (!BenchmarkSuite.class.isAssignableFrom(suiteClass)) {
        throw new ConfigurationException(suiteClass + " is not a benchmark suite.");
      }

      Constructor<? extends BenchmarkSuite> constructor
          = suiteClass.getDeclaredConstructor();
      suite = constructor.newInstance();
    } catch (InvocationTargetException e) {
      throw new ExecutionException(e.getCause());
    } catch (Exception e) {
      throw new ConfigurationException(e);
    }
  }

  private void run() {
    Collection<Run> runs = suite.createRuns();
    System.out.println(runs.size() + " runs...");
    for (Run run : runs) {
      execute(run);
    }
  }

  private void execute(Run run) {
    try {
      System.out.println(run);
      run.getBenchmarkSuite().setUp();
      long start = System.nanoTime();
      run.getBenchmark().run(10000);
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
    System.out.println("Usage: Runner <benchmark class>");
  }

  public static void main(String... args) {
    Runner runner = new Runner();
    if (!runner.parseArgs(args)) {
      runner.printUsage();
      return;
    }

    runner.prepare();
    runner.run();
  }
}
