/*
 * Copyright (C) 2011 Google Inc.
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

package com.google.caliper.runner;

import com.google.caliper.api.Benchmark;
import com.google.caliper.api.Param;
import com.google.caliper.api.SkipThisScenarioException;
import com.google.caliper.api.VmParam;
import com.google.common.collect.ImmutableSortedMap;
import com.google.common.collect.Sets;

import java.lang.reflect.Method;
import java.util.Set;

/**
 * An instance of this type represents a user-provided class that extends Benchmark. It manages
 * creating, setting up and destroying instances of that class.
 */
public final class BenchmarkClass {
  private final Class<? extends Benchmark> theClass;
  private final ParameterSet<Object> userParameters;
  private final ParameterSet<String> injectableVmArguments;

  public BenchmarkClass(Class<?> aClass)
      throws InvalidBenchmarkException {
    try {
      this.theClass = aClass.asSubclass(Benchmark.class);
    } catch (ClassCastException e) {
      throw new InvalidBenchmarkException("Does not extend Benchmark: " + aClass.getName());
    }
    this.userParameters = ParameterSet.create(theClass, Param.class);
    this.injectableVmArguments = ParameterSet.create(theClass, VmParam.class, String.class);

    validate();
  }

  private void validate() throws InvalidBenchmarkException {
    Set<String> both = Sets.intersection(
        userParameters.names(),
        injectableVmArguments.names());
    if (!both.isEmpty()) {
      throw new InvalidBenchmarkException("fields are both @Param and @VmParam: " + both);
    }
  }

  public ParameterSet<Object> userParameters() {
    return userParameters;
  }

  public ParameterSet<String> injectableVmArguments() {
    return injectableVmArguments;
  }

  public ImmutableSortedMap<String, BenchmarkMethod> findAllBenchmarkMethods(
      Instrument instrument) throws InvalidBenchmarkException {
    ImmutableSortedMap.Builder<String, BenchmarkMethod> result = ImmutableSortedMap.naturalOrder();
    for (Method method : theClass.getDeclaredMethods()) {
      if (instrument.isBenchmarkMethod(method)) {
        BenchmarkMethod benchmarkMethod = instrument.createBenchmarkMethod(this, method);
        result.put(benchmarkMethod.name(), benchmarkMethod);
      }
    }
    return result.build();
  }

  public Benchmark createAndStage(Scenario scenario) throws InvalidBenchmarkException {
    Benchmark benchmark = createBenchmarkInstance(theClass);
    userParameters.injectAll(benchmark, scenario.userParameters());
    injectableVmArguments.injectAll(benchmark, scenario.vmArguments());

    try {
      benchmark.setUp();
    } catch (SkipThisScenarioException e) {
      throw e;
    } catch (Exception e) {
      throw new UserCodeException(e);
    }
    return benchmark;
  }

  private static Benchmark createBenchmarkInstance(Class<? extends Benchmark> theClass)
      throws InvalidBenchmarkException {
    Benchmark benchmark;
    try {
      benchmark = theClass.newInstance();

    } catch (InstantiationException e) {
      throw new InvalidBenchmarkException("Class is abstract: " + theClass.getName());
    } catch (IllegalAccessException e) {
      throw new InvalidBenchmarkException(
          "Not public or lacks public constructor: " + theClass.getName());
    } catch (Exception e) {
      throw new UserCodeException(e);
    }
    return benchmark;
  }

  @Override public boolean equals(Object object) {
    if (object instanceof BenchmarkClass) {
      BenchmarkClass that = (BenchmarkClass) object;
      return this.theClass.equals(that.theClass);
    }
    return false;
  }

  @Override public int hashCode() {
    return theClass.hashCode();
  }

  @Override public String toString() {
    return theClass.getName();
  }
}
