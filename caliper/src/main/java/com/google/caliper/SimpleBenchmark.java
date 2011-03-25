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
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/**
 * A convenience class for implementing benchmarks in plain code.
 * Implementing classes must have a no-arguments constructor.
 *
 * <h3>Benchmarks</h3>
 * The benchmarks of a suite are defined by . They may be
 * static. They are not permitted to take parameters . . ..
 *
 * <h3>Parameters</h3>
 * See the {@link Param} documentation to learn about parameters.
 */
public abstract class SimpleBenchmark
    extends com.google.caliper.api.Benchmark // TEMPORARY for transition
    implements Benchmark {
  private static final Class<?>[] ARGUMENT_TYPES = { int.class };

  private final Map<String, Parameter<?>> parameters;
  private final Map<String, Method> methods;

  protected SimpleBenchmark() {
    parameters = Parameter.forClass(getClass());
    methods = createTimedMethods();

    if (methods.isEmpty()) {
      throw new ConfigurationException(
          "No benchmarks defined in " + getClass().getName());
    }
  }

  protected void setUp() throws Exception {}

  protected void tearDown() throws Exception {}

  @Override public Set<String> parameterNames() {
    return ImmutableSet.<String>builder()
        .add("benchmark")
        .addAll(parameters.keySet())
        .build();
  }

  @Override public Set<String> parameterValues(String parameterName) {
    if ("benchmark".equals(parameterName)) {
      return methods.keySet();
    }

    Parameter<?> parameter = parameters.get(parameterName);
    if (parameter == null) {
      throw new IllegalArgumentException();
    }
    try {
      Iterable<?> values = parameter.values();

      ImmutableSet.Builder<String> result = ImmutableSet.builder();
      for (Object value : values) {
        result.add(String.valueOf(value));
      }
      return result.build();
    } catch (Exception e) {
      throw new ExceptionFromUserCodeException(e);
    }
  }

  @Override public ConfiguredBenchmark createBenchmark(Map<String, String> parameterValues) {
    if (!parameterNames().equals(parameterValues.keySet())) {
      throw new IllegalArgumentException("Invalid parameters specified. Expected "
          + parameterNames() + " but was " + parameterValues.keySet());
    }

    String methodName = parameterValues.get("benchmark");
    final Method method = methods.get(methodName);
    if (method == null) {
      throw new IllegalArgumentException("Invalid parameters specified. \"time" + methodName + "\" "
          + "is not a method of this benchmark.");
    }

    try {
      @SuppressWarnings({"ClassNewInstance"}) // can throw any Exception, so we catch all Exceptions
      final SimpleBenchmark copyOfSelf = getClass().newInstance();

      for (Map.Entry<String, String> entry : parameterValues.entrySet()) {
        String parameterName = entry.getKey();
        if ("benchmark".equals(parameterName)) {
          continue;
        }

        Parameter<?> parameter = parameters.get(parameterName);
        Object value = TypeConverter.fromString(entry.getValue(), parameter.getType());
        parameter.set(copyOfSelf, value);
      }
      copyOfSelf.setUp();

      return new ConfiguredBenchmark(copyOfSelf) {
        @Override public Object run(int reps) throws Exception {
          try {
            return method.invoke(copyOfSelf, reps);
          } catch (InvocationTargetException e) {
            Throwable cause = e.getCause();
            if (cause instanceof Exception) {
              throw (Exception) cause;
            } else if (cause instanceof Error) {
              throw (Error) cause;
            } else {
              throw e;
            }
          }
        }

        @Override public void close() throws Exception {
          copyOfSelf.tearDown();
        }
      };
    } catch (Exception e) {
      throw new ExceptionFromUserCodeException(e);
    }
  }

  public Scenario normalizeScenario(Scenario scenario) {
    Map<String, String> variables =
      new LinkedHashMap<String, String>(scenario.getVariables());
    // Make sure the scenario contains method names without the prefixed "time". If
    // it has "time" prefixed, then remove it. Also check whether the user has
    // accidentally put a lower cased letter first, and fix it if necessary.
    String benchmark = variables.get("benchmark");
    Map<String, Method> timedMethods = createTimedMethods();
    if (timedMethods.get(benchmark) == null) {
      // try to upper case first character
      char[] benchmarkChars = benchmark.toCharArray();
      benchmarkChars[0] = Character.toUpperCase(benchmarkChars[0]);
      String upperCasedBenchmark = String.valueOf(benchmarkChars);
      if (timedMethods.get(upperCasedBenchmark) != null) {
        variables.put("benchmark", upperCasedBenchmark);
      } else if (benchmark.startsWith("time")) {
        variables.put("benchmark", benchmark.substring(4));
      }
    }
    return new Scenario(variables);
  }

  /**
   * Returns a spec for each benchmark defined in the specified class. The
   * returned specs have no parameter values; those must be added separately.
   */
  private Map<String, Method> createTimedMethods() {
    ImmutableMap.Builder<String, Method> result = ImmutableMap.builder();
    for (Method method : getClass().getDeclaredMethods()) {
      int modifiers = method.getModifiers();
      if (!method.getName().startsWith("time")) {
        continue;
      }

      if (!Modifier.isPublic(modifiers)
          || Modifier.isStatic(modifiers)
          || Modifier.isAbstract(modifiers)
          || !Arrays.equals(method.getParameterTypes(), ARGUMENT_TYPES)) {
        throw new ConfigurationException("Timed methods must be public, "
            + "non-static, non-abstract and take a single int parameter. "
            + "But " + method + " violates these requirements.");
      }

      result.put(method.getName().substring(4), method);
    }

    return result.build();
  }

  @Override public Map<String, Integer> getTimeUnitNames() {
    return ImmutableMap.of("ns", 1,
        "us", 1000,
        "ms", 1000000,
        "s", 1000000000);
  }

  @Override public double nanosToUnits(double nanos) {
    return nanos;
  }

  @Override public Map<String, Integer> getInstanceUnitNames() {
    return ImmutableMap.of(" instances", 1,
        "K instances", 1000,
        "M instances", 1000000,
        "B instances", 1000000000);
  }

  @Override public double instancesToUnits(long instances) {
    return instances;
  }

  @Override public Map<String, Integer> getMemoryUnitNames() {
    return ImmutableMap.of("B", 1,
        "KiB", 1024,
        "MiB", 1048576,
        "GiB", 1073741824);
  }

  @Override public double bytesToUnits(long bytes) {
    return bytes;
  }
}