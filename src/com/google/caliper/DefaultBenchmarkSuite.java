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

  /**
   * Combines parameter values and code to build a benchmark instance.
   */
  static abstract class Spec implements Cloneable {
    List<Object> paramValues = new ArrayList<Object>();

    abstract Benchmark create(BenchmarkSuite suite) throws Exception;

    @Override protected Spec clone() {
      try {
        Spec result = (Spec) super.clone();
        result.paramValues = new ArrayList<Object>(paramValues);
        return result;
      } catch (CloneNotSupportedException e) {
        throw new AssertionError(e);
      }
    }
  }

  @Override protected Collection<Run> createRuns() {
    Class<? extends BenchmarkSuite> suiteClass = getClass();
    List<Spec> specs = createSpecs(suiteClass);

    // prepare the parameters
    List<Parameter<?>> parameters = Parameter.forClass(suiteClass);
    try {
      specCartesianProduct(specs, parameters);
    } catch (Exception e) {
      throw new ExecutionException(e);
    }

    // use the parameters to create runs
    List<Run> runs = new ArrayList<Run>();
    for (Spec spec : specs) {
      try {
        BenchmarkSuite suite = suiteClass.newInstance();
        Map<String, String> parametersForRun = new LinkedHashMap<String, String>();

        for (int i = 0; i < parameters.size(); i++) {
          @SuppressWarnings("unchecked") // guarded by parameter setup
          Parameter<Object> parameter = (Parameter<Object>) parameters.get(i);
          Object value = spec.paramValues.get(i);
          parameter.set(suite, value);
          parametersForRun.put(parameter.getName(), String.valueOf(value));
        }

        Benchmark benchmark = spec.create(suite);
        runs.add(new Run(suite, benchmark, parametersForRun));
      } catch (Exception e) {
        throw new ExecutionException(e);
      }
    }
    return runs;
  }

  /**
   * Grows the specs list and the parameters list in each spec, until a spec
   * with every complete combination of parameter values is included in the
   * list.
   */
  private void specCartesianProduct(List<Spec> specs, List<Parameter<?>> parameters)
      throws Exception {
    for (int p = 0, parametersSize = parameters.size(); p < parametersSize; p++) {
      Parameter<?> parameter = parameters.get(p);
      Iterator<?> values = parameter.values().iterator();
      if (!values.hasNext()) {
        throw new ConfigurationException("Not enough values for " + parameter);
      }

      // add the first value to all specs
      Object value = values.next();
      for (Spec run : specs) {
        run.paramValues.add(p, value);
      }

      // multiply the size of the specs by the number of alternate values
      int length = specs.size();
      while (values.hasNext()) {
        Object alternate = values.next();
        for (int s = 0; s < length; s++) {
          Spec copy = specs.get(s).clone();
          copy.paramValues.set(p, alternate);
          specs.add(copy);
        }
      }
    }
  }

  /**
   * Returns a spec for each benchmark defined in the specified class. The
   * returned specs have no parameter values; those must be added separately.
   */
  private List<Spec> createSpecs(Class<? extends BenchmarkSuite> suiteClass) {
    List<Spec> result = new ArrayList<Spec>();
    for (Class<?> c : suiteClass.getDeclaredClasses()) {
      if (!Benchmark.class.isAssignableFrom(c) || c.isInterface()) {
        continue;
      }

      @SuppressWarnings("unchecked") // guarded by isAssignableFrom
      Class<? extends Benchmark> driverClass = (Class<? extends Benchmark>) c;

      try {
        final Constructor<? extends Benchmark> constructor = driverClass.getDeclaredConstructor();
        result.add(new Spec() {
          public Benchmark create(BenchmarkSuite suite) throws Exception {
            return constructor.newInstance();
          }
        });
        continue;
      } catch (NoSuchMethodException ignored) {
      }

      try {
        final Constructor<? extends Benchmark> constructor = driverClass.getDeclaredConstructor(suiteClass);
        result.add(new Spec() {
          public Benchmark create(BenchmarkSuite suite) throws Exception {
            return constructor.newInstance(suite);
          }
        });
        continue;
      } catch (NoSuchMethodException ignored) {
      }

      throw new ConfigurationException("No usable constructor for "
          + driverClass.getName() + "\n  Drivers may only use no arguments constructors.");
    }

    if (result.isEmpty()) {
      throw new ConfigurationException("No drivers defined in " + suiteClass);
    }

    return result;
  }
}
