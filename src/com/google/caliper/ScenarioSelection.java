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

import com.google.caliper.UserException.AbstractBenchmarkException;
import com.google.caliper.UserException.DoesntImplementBenchmarkException;
import com.google.caliper.UserException.ExceptionFromUserCodeException;
import com.google.caliper.UserException.NoParameterlessConstructorException;
import com.google.caliper.UserException.NoSuchClassException;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.LinkedHashMultimap;
import com.google.common.collect.Multimap;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

/**
 * Figures out which scenarios to benchmark given a benchmark suite, set of user
 * parameters, and set of user VMs.
 */
public final class ScenarioSelection {

  private final String suiteClassName;
  private final Multimap<String, String> userParameters;
  private final Set<String> userVms;
  private final int trials;

  private Benchmark suite;

  /** Effective parameters to run in the benchmark. */
  private final Multimap<String, String> parameters = LinkedHashMultimap.create();

  public ScenarioSelection(Arguments arguments) {
    this(arguments.getSuiteClassName(), arguments.getUserParameters(), arguments.getUserVms(), arguments.getTrials());
  }

  public ScenarioSelection(String suiteClassName,
      Multimap<String, String> userParameters, Set<String> userVms, int trials) {
    this.suiteClassName = suiteClassName;
    this.userParameters = userParameters;
    this.userVms = userVms;
    this.trials = trials;
  }

  /**
   * Returns the selected scenarios for this benchmark.
   */
  public List<Scenario> select() {
    prepareSuite();
    prepareParameters();
    return createScenarios();
  }

  /**
   * Returns a normalized version of {@code scenario}, with information from {@code suite}
   * assisting in correcting problems.
   */
  public Scenario normalizeScenario(Scenario scenario) {
    // This only applies to SimpleBenchmarks since they accept the special "benchmark"
    // parameter. This is a special case because SimpleBenchmark is the most commonly
    // used benchmark class. Have to do this horrible stuff since Benchmark API
    // doesn't provide scenario-normalization (and it shouldn't), which SimpleBenchmark
    // requires.
    if (suite instanceof SimpleBenchmark) {
      return ((SimpleBenchmark) suite).normalizeScenario(scenario);
    }

    return scenario;
  }

  public ConfiguredBenchmark createBenchmark(Scenario scenario) {
    return suite.createBenchmark(scenario.getParameters());
  }

  private void prepareSuite() {
    Class<?> benchmarkClass;
    try {
      benchmarkClass = getClassByName(suiteClassName);
    } catch (ExceptionInInitializerError e) {
      throw new ExceptionFromUserCodeException(e.getCause());
    } catch (ClassNotFoundException ignored) {
      throw new NoSuchClassException(suiteClassName);
    }

    Object s;
    try {
      Constructor<?> constructor = benchmarkClass.getDeclaredConstructor();
      constructor.setAccessible(true);
      s = constructor.newInstance();
    } catch (InstantiationException ignore) {
      throw new AbstractBenchmarkException(benchmarkClass);
    } catch (NoSuchMethodException ignore) {
      throw new NoParameterlessConstructorException(benchmarkClass);
    } catch (IllegalAccessException impossible) {
      throw new AssertionError(impossible); // shouldn't happen since we setAccessible(true)
    } catch (InvocationTargetException e) {
      throw new ExceptionFromUserCodeException(e.getCause());
    }

    if (s instanceof Benchmark) {
      this.suite = (Benchmark) s;
    } else {
      throw new DoesntImplementBenchmarkException(benchmarkClass);
    }
  }

  private static Class<?> getClassByName(String className) throws ClassNotFoundException {
    try {
      return Class.forName(className);
    } catch (ClassNotFoundException ignored) {
      // try replacing the last dot with a $, in case that helps
      // example: tutorial.Tutorial.Benchmark1 becomes tutorial.Tutorial$Benchmark1
      // amusingly, the $ character means three different things in this one line alone
      String newName = className.replaceFirst("\\.([^.]+)$", "\\$$1");
      return Class.forName(newName);
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
          throw new ConfigurationException(key + " has no values. "
              + "Did you forget a -D" + key + "=<value> command line argument?");
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
   * Returns a complete set of scenarios with every combination of values and
   * benchmark classes.
   */
  private List<Scenario> createScenarios() {
    List<ScenarioBuilder> builders = new ArrayList<ScenarioBuilder>();

    // create scenarios for each VM
    Set<String> vms = userVms.isEmpty()
        ? defaultVms()
        : userVms;
    for (String vm : vms) {
      for (int i = 0; i < trials; i++) {
        ScenarioBuilder scenarioBuilder = new ScenarioBuilder();
        scenarioBuilder.parameters.put(Scenario.VM_KEY, vm);
        scenarioBuilder.parameters.put(Scenario.TRIAL_KEY, "" + i);
        builders.add(scenarioBuilder);
      }
    }

    for (Entry<String, Collection<String>> parameter : parameters.asMap().entrySet()) {
      Iterator<String> values = parameter.getValue().iterator();
      if (!values.hasNext()) {
        throw new ConfigurationException("Not enough values for " + parameter);
      }

      String key = parameter.getKey();

      String firstValue = values.next();
      for (ScenarioBuilder builder : builders) {
        builder.parameters.put(key, firstValue);
      }

      // multiply the size of the specs by the number of alternate values
      int size = builders.size();
      while (values.hasNext()) {
        String alternate = values.next();
        for (int s = 0; s < size; s++) {
          ScenarioBuilder copy = builders.get(s).copy();
          copy.parameters.put(key, alternate);
          builders.add(copy);
        }
      }
    }

    List<Scenario> result = new ArrayList<Scenario>();
    for (ScenarioBuilder builder : builders) {
      result.add(normalizeScenario(builder.build()));
    }

    return result;
  }

  private static class ScenarioBuilder {
    final Map<String, String> parameters = new LinkedHashMap<String, String>();

    ScenarioBuilder copy() {
      ScenarioBuilder result = new ScenarioBuilder();
      result.parameters.putAll(parameters);
      return result;
    }

    public Scenario build() {
      return new Scenario(parameters);
    }
  }
}
