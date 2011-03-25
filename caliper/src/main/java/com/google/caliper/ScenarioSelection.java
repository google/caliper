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

  private final Set<String> userVms;
  private final Multimap<String, String> vmParameters;
  private final String suiteClassName;

  /**
   * The user parameters specified on the command line. This may be a subset of
   * the effective user parameters because parameters not specified here may get
   * default values from the benchmark class.
   */
  private final Multimap<String, String> userParameterArguments;

  /**
   * The actual user parameters we'll use to run in the benchmark. This contains
   * the userParameterArguments plus the default user parameters.
   */
  private Multimap<String, String> userParameters;

  private final int trials;
  private Benchmark suite;


  public ScenarioSelection(Arguments arguments) {
    this(arguments.getUserVms(), arguments.getVmParameters(), arguments.getSuiteClassName(),
        arguments.getUserParameters(), arguments.getTrials());
  }

  public ScenarioSelection(Set<String> userVms, Multimap<String, String> vmParameters,
      String suiteClassName, Multimap<String, String> userParameterArguments, int trials) {
    this.userVms = userVms;
    this.vmParameters = vmParameters;
    this.suiteClassName = suiteClassName;
    this.userParameterArguments = userParameterArguments;
    this.trials = trials;
  }

  /**
   * Returns the selected scenarios for this benchmark.
   */
  public List<Scenario> select() {
    prepareSuite();
    userParameters = computeUserParameters();
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

  public Set<String> getUserParameterNames() {
    if (userParameters == null) {
      throw new IllegalStateException();
    }
    return userParameters.keySet();
  }

  public Set<String> getVmParameterNames() {
    return vmParameters.keySet();
  }

  public ConfiguredBenchmark createBenchmark(Scenario scenario) {
    return suite.createBenchmark(scenario.getVariables(getUserParameterNames()));
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

  private Multimap<String, String> computeUserParameters() {
    Multimap<String, String> result = LinkedHashMultimap.create();
    for (String key : suite.parameterNames()) {
      // first check if the user has specified values
      Collection<String> userValues = userParameterArguments.get(key);
      if (!userValues.isEmpty()) {
        result.putAll(key, userValues);
        // TODO: type convert 'em to validate?

      } else { // otherwise use the default values from the suite
        Set<String> values = suite.parameterValues(key);
        if (values.isEmpty()) {
          throw new ConfigurationException(key + " has no values. "
              + "Did you forget a -D" + key + "=<value> command line argument?");
        }
        result.putAll(key, values);
      }
    }
    return result;
  }

  /**
   * Returns a complete set of scenarios with every combination of variables.
   */
  private List<Scenario> createScenarios() {
    List<ScenarioBuilder> builders = new ArrayList<ScenarioBuilder>();
    builders.add(new ScenarioBuilder());

    Map<String, Collection<String>> variables = new LinkedHashMap<String, Collection<String>>();
    variables.put(Scenario.VM_KEY, userVms.isEmpty() ? VmFactory.defaultVms() : userVms);
    variables.put(Scenario.TRIAL_KEY, newListOfSize(trials));
    variables.putAll(userParameters.asMap());
    variables.putAll(vmParameters.asMap());

    for (Entry<String, Collection<String>> entry : variables.entrySet()) {
      Iterator<String> values = entry.getValue().iterator();
      if (!values.hasNext()) {
        throw new ConfigurationException("Not enough values for " + entry);
      }

      String firstValue = values.next();
      for (ScenarioBuilder builder : builders) {
        builder.variables.put(entry.getKey(), firstValue);
      }

      // multiply the size of the specs by the number of alternate values
      int size = builders.size();
      while (values.hasNext()) {
        String alternate = values.next();
        for (int s = 0; s < size; s++) {
          ScenarioBuilder copy = builders.get(s).copy();
          copy.variables.put(entry.getKey(), alternate);
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

  /**
   * Returns a list containing {@code count} distinct elements.
   */
  private Collection<String> newListOfSize(int count) {
    List<String> result = new ArrayList<String>();
    for (int i = 0; i < count; i++) {
      result.add(Integer.toString(i));
    }
    return result;
  }

  private static class ScenarioBuilder {
    final Map<String, String> variables = new LinkedHashMap<String, String>();

    ScenarioBuilder copy() {
      ScenarioBuilder result = new ScenarioBuilder();
      result.variables.putAll(variables);
      return result;
    }

    public Scenario build() {
      return new Scenario(variables);
    }
  }
}
