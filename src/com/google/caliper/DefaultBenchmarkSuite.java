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
import java.lang.reflect.Type;
import java.util.*;

/**
 * A convenience class for implementing benchmark suites in plain code.
 * Implementing classes must have a no-arguments constructor.
 *
 * <h3>Benchmarks</h3>
 * The benchmarks of a suite are defined by inner classes within the suite.
 * These inner classes implement the {@link Benchmark} interface. They may be
 * static. They are not permitted to take parameters in their constructors.
 *
 * <h3>Parameters</h3>
 * Implementing classes may be configured using parameters. Each parameter is a
 * property of a benchmark, plus the default values that fulfill it. Parameters
 * are specified by annotated fields:
 * <pre>{@code
 *   &#64;Param int length;
 * }</pre>
 * The available values for a parameter are specified by another field with the
 * same name plus the {@code Values} suffix. The type of this field must be an
 * {@code Iterable} of the parameter's type.
 * <pre>{@code
 *   Iterable<Integer> lengthValues = Arrays.asList(10, 100, 1000, 10000);
 * }</pre>
 * Alternatively, the available values may be specified with a method. The
 * method's name follows the same naming convention and returns the same type.
 * Such methods may not accept parameters of their own.
 * <pre>{@code
 *   Iterable<Integer> lengthValues() {
 *     return Arrays.asList(10, 100, 1000, 10000);
 *   }
 * }</pre>
 */
public abstract class DefaultBenchmarkSuite extends BenchmarkSuite {

  private final Map<String, Parameter<?>> parameters;
  private final Map<Class<? extends Benchmark>, BenchmarkFactory> benchmarkFactories;

  protected void setUp() throws Exception {}

  protected DefaultBenchmarkSuite() {
    parameters = Parameter.forClass(getClass());
    benchmarkFactories = createBenchmarkFactories();

    if (benchmarkFactories.isEmpty()) {
      throw new ConfigurationException(
          "No benchmarks defined in " + getClass().getName());
    }
  }

  protected Set<Class<? extends Benchmark>> benchmarkClasses() {
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

  protected Benchmark createBenchmark(Class<? extends Benchmark> benchmarkClass,
      Map<String, String> parameterValues) {
    TypeConverter typeConverter = new TypeConverter();

    BenchmarkFactory benchmarkFactory = benchmarkFactories.get(benchmarkClass);
    if (benchmarkFactory == null) {
      throw new IllegalArgumentException();
    }

    if (!parameters.keySet().equals(parameterValues.keySet())) {
      throw new IllegalArgumentException("Invalid parameters specified. Expected "
          + parameters.keySet() + " but was " + parameterValues.keySet());
    }

    try {
      DefaultBenchmarkSuite copyOfSelf = getClass().newInstance();
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
  private Map<Class<? extends Benchmark>, BenchmarkFactory> createBenchmarkFactories() {
    Map<Class<? extends Benchmark>, BenchmarkFactory> result
        = new LinkedHashMap<Class<? extends Benchmark>, BenchmarkFactory>();
    for (Class<?> c : getClass().getDeclaredClasses()) {
      if (!Benchmark.class.isAssignableFrom(c) || c.isInterface()) {
        continue;
      }

      @SuppressWarnings("unchecked") // guarded by isAssignableFrom
      Class<? extends Benchmark> benchmarkClass = (Class<? extends Benchmark>) c;

      try {
        final Constructor<? extends Benchmark> constructor = benchmarkClass.getDeclaredConstructor();
        result.put(benchmarkClass, new BenchmarkFactory() {
          public Benchmark create(BenchmarkSuite suite) throws Exception {
            return constructor.newInstance();
          }
        });
        continue;
      } catch (NoSuchMethodException ignored) {
      }

      try {
        final Constructor<? extends Benchmark> constructor
            = benchmarkClass.getDeclaredConstructor(getClass());
        result.put(benchmarkClass, new BenchmarkFactory() {
          public Benchmark create(BenchmarkSuite suite) throws Exception {
            return constructor.newInstance(suite);
          }
        });
        continue;
      } catch (NoSuchMethodException ignored) {
      }

      throw new ConfigurationException("No usable constructor for "
          + benchmarkClass.getName() + "\n  Benchmarks may only use no arguments constructors.");
    }

    return result;
  }

  interface BenchmarkFactory {
    Benchmark create(BenchmarkSuite suite) throws Exception;
  }
}
