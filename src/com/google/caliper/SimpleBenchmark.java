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

import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.*;

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
public abstract class SimpleBenchmark extends BenchmarkSuite {

  private final Map<String, Parameter<?>> parameters;
  private final Map<Method, BenchmarkFactory> benchmarkFactories;

  protected void setUp() throws Exception {}

  protected SimpleBenchmark() {
    parameters = Parameter.forClass(getClass());
    benchmarkFactories = createBenchmarkFactories();

    if (benchmarkFactories.isEmpty()) {
      throw new ConfigurationException(
          "No benchmarks defined in " + getClass().getName());
    }
  }

  protected Set<Method> benchmarkMethods() {
    return benchmarkFactories.keySet();
  }

  protected Set<String> parameterNames() {
    return parameters.keySet();
  }

  protected Set<String> parameterValues(String parameterName) {
    try {
      TypeConverter typeConverter = new TypeConverter();
      Parameter<?> parameter = parameters.get(parameterName);
      if (parameter == null) {
        throw new IllegalArgumentException();
      }
      Collection<?> values = parameter.values();
      Type type = parameter.getType();
      Set<String> result = new LinkedHashSet<String>();
      for (Object value : values) {
        result.add(typeConverter.toString(value, type));
      }
      return result;
    } catch (Exception e) {
      throw new ExecutionException(e);
    }
  }

  protected Benchmark createBenchmark(Method benchmarkMethod,
      Map<String, String> parameterValues) {
    TypeConverter typeConverter = new TypeConverter();

    BenchmarkFactory benchmarkFactory = benchmarkFactories.get(benchmarkMethod);
    if (benchmarkFactory == null) {
      throw new IllegalArgumentException();
    }

    if (!parameters.keySet().equals(parameterValues.keySet())) {
      throw new IllegalArgumentException("Invalid parameters specified. Expected "
          + parameters.keySet() + " but was " + parameterValues.keySet());
    }

    try {
      SimpleBenchmark copyOfSelf = getClass().newInstance();
      Benchmark benchmark = benchmarkFactory.create(copyOfSelf);
      for (Map.Entry<String, String> entry : parameterValues.entrySet()) {
        Parameter parameter = parameters.get(entry.getKey());
        Object value = typeConverter.fromString(entry.getValue(), parameter.getType());
        parameter.set(copyOfSelf, value);
      }

      copyOfSelf.setUp();
      return benchmark;

    } catch (Exception e) {
      throw new ExecutionException(e);
    }
  }

  /**
   * Returns a spec for each benchmark defined in the specified class. The
   * returned specs have no parameter values; those must be added separately.
   */
  private Map<Method, BenchmarkFactory> createBenchmarkFactories() {
    Map<Method, BenchmarkFactory> result
        = new LinkedHashMap<Method, BenchmarkFactory>();
    for (final Method method : getClass().getDeclaredMethods()) {
      if (!ReflectiveBenchmark.isBenchmarkMethod(method)) {
        continue;
      }

      result.put(method, new BenchmarkFactory() {
        public Benchmark create(BenchmarkSuite suite) throws Exception {
          return new ReflectiveBenchmark((SimpleBenchmark) suite, method);
        }
      });
    }

    return result;
  }

  interface BenchmarkFactory {
    Benchmark create(BenchmarkSuite suite) throws Exception;
  }
}