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
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.Collection;
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
 * Implementing classes may be configured using parameters. Each parameter is a
 * property of a benchmark, plus the default values that fulfill it. Parameters
 * are specified by annotated fields:
 * <pre>
 *   {@literal @}Param int length;
 * </pre>
 * The available values for a parameter are specified by another field with the
 * same name plus the {@code Values} suffix. The type of this field must be an
 * {@code Iterable} of the parameter's type.
 * <pre>
 *   Iterable&lt;Integer&gt; lengthValues = Arrays.asList(10, 100, 1000, 10000);
 * </pre>
 * Alternatively, the available values may be specified with a method. The
 * method's name follows the same naming convention and returns the same type.
 * Such methods may not accept parameters of their own.
 * <pre>
 *   Iterable&lt;Integer&gt; lengthValues() {
 *     return Arrays.asList(10, 100, 1000, 10000);
 *   }
 * </pre>
 */
public abstract class SimpleBenchmark implements Benchmark {

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

  public Set<String> parameterNames() {
    return ImmutableSet.<String>builder()
        .add("benchmark")
        .addAll(parameters.keySet())
        .build();
  }

  public Set<String> parameterValues(String parameterName) {
    if ("benchmark".equals(parameterName)) {
      return methods.keySet();
    }

    Parameter<?> parameter = parameters.get(parameterName);
    if (parameter == null) {
      throw new IllegalArgumentException();
    }
    try {
      Collection<?> values = parameter.values();

      ImmutableSet.Builder<String> result = ImmutableSet.builder();
      for (Object value : values) {
        result.add(String.valueOf(value));
      }
      return result.build();
    } catch (Exception e) {
      throw new ExecutionException(e);
    }
  }

  public TimedRunnable createBenchmark(Map<String, String> parameterValues) {
    if (!parameterNames().equals(parameterValues.keySet())) {
      throw new IllegalArgumentException("Invalid parameters specified. Expected "
          + parameterNames() + " but was " + parameterValues.keySet());
    }

    try {
      @SuppressWarnings({"ClassNewInstance"}) // can throw any Exception, so we catch all Exceptions
      final SimpleBenchmark copyOfSelf = getClass().newInstance();
      final Method method = methods.get(parameterValues.get("benchmark"));

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

      return new TimedRunnable() {
        public Object run(int reps) throws Exception {
          return method.invoke(copyOfSelf, reps);
        }
      };

    } catch (Exception e) {
      throw new ExecutionException(e);
    }
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
}