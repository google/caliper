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

import static com.google.common.base.Throwables.propagateIfInstanceOf;

import com.google.caliper.Param;
import com.google.caliper.api.Benchmark;
import com.google.caliper.api.SkipThisScenarioException;
import com.google.caliper.api.VmParam;
import com.google.caliper.util.InvalidCommandException;
import com.google.common.collect.ImmutableSetMultimap;
import com.google.common.collect.ImmutableSortedMap;
import com.google.common.collect.Sets;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Set;

/**
 * An instance of this type represents a user-provided class that extends Benchmark. It manages
 * creating, setting up and destroying instances of that class.
 */
public final class BenchmarkClass {
  private final Class<? extends Benchmark> theClass;
  private final Constructor<? extends Benchmark> constructor;
  private final ParameterSet userParameters;
  private final ParameterSet injectableVmArguments;

  public BenchmarkClass(Class<?> aClass) throws InvalidBenchmarkException {
    if (Modifier.isAbstract(aClass.getModifiers())) {
      throw new InvalidBenchmarkException("Class '%s' is abstract", aClass);
    }
    
    // TODO: check for nested, non-static classes (non-abstract, but no constructor?)
    // this will fail later anyway (no way to declare parameterless nested constr., but
    // maybe signal this better?

    try {
      this.theClass = aClass.asSubclass(Benchmark.class);
    } catch (ClassCastException e) {
      throw new InvalidBenchmarkException(
          "Class '%s' does not extend %s", aClass, Benchmark.class.getName());
    }

    // TODO(kevinb): check that it's a *direct* subclass, because semantics of @Params and such
    // are too much of a pain to specify otherwise.

    this.constructor = findConstructor(theClass);

    this.userParameters = ParameterSet.create(theClass, Param.class);
    this.injectableVmArguments = ParameterSet.create(theClass, VmParam.class);

    validate();
  }

  private void validate() throws InvalidBenchmarkException {
    Set<String> both = Sets.intersection(userParameters.names(), injectableVmArguments.names());
    if (!both.isEmpty()) {
      throw new InvalidBenchmarkException("Some fields have both @Param and @VmParam: " + both);
    }
  }

  public ParameterSet userParameters() {
    return userParameters;
  }

  public ParameterSet injectableVmArguments() {
    return injectableVmArguments;
  }

  // TODO: perhaps move this to Instrument and let instruments override it?

  public ImmutableSortedMap<String, BenchmarkMethod> findAllBenchmarkMethods(
      Instrument instrument) throws InvalidBenchmarkException {
    boolean gotOne = false;
    ImmutableSortedMap.Builder<String, BenchmarkMethod> result = ImmutableSortedMap.naturalOrder();
    for (Method method : theClass.getDeclaredMethods()) {
      if (instrument.isBenchmarkMethod(method)) {
        BenchmarkMethod benchmarkMethod = instrument.createBenchmarkMethod(this, method);
        result.put(benchmarkMethod.name(), benchmarkMethod);
        gotOne = true;
      }
    }
    if (!gotOne) {
      throw new InvalidBenchmarkException(
          "Class '%s' contains no benchmark methods for instrument '%s'", theClass, instrument);
    }
    return result.build();
  }

  public Benchmark createAndStage(Scenario scenario) throws UserCodeException {
    Benchmark benchmark = createBenchmarkInstance(constructor);
    userParameters.injectAll(benchmark, scenario.userParameters());
    injectableVmArguments.injectAll(benchmark, scenario.vmArguments());

    callSetUp(benchmark);
    return benchmark;
  }

  public String name() {
    return theClass.getName();
  }

  @Override public String toString() {
    return name();
  }

  // We have to do this reflectively because it'd be too much of a pain to make setUp public
  private void callSetUp(Benchmark benchmark) throws UserCodeException {
    try {
      SETUP_METHOD.invoke(benchmark);

    } catch (IllegalAccessException e) {
      throw new AssertionError(e);

    } catch (InvocationTargetException e) {
      propagateIfInstanceOf(e.getCause(), SkipThisScenarioException.class);
      throw new UserCodeException("Exception thrown during setUp", e.getCause());
    }
  }

  private static final Method SETUP_METHOD = findSetUpMethod();

  private static Method findSetUpMethod() {
    try {
      Method setUp = Benchmark.class.getDeclaredMethod("setUp");
      setUp.setAccessible(true);
      return setUp;
    } catch (NoSuchMethodException e) {
      throw new AssertionError(e);
    }
  }

  private static Constructor<? extends Benchmark> findConstructor(
      Class<? extends Benchmark> theClass) throws InvalidBenchmarkException {
    Constructor<? extends Benchmark> constructor;
    try {
      constructor = theClass.getDeclaredConstructor();
    } catch (NoSuchMethodException e) {
      throw new InvalidBenchmarkException(
          "Class '%s' has no parameterless constructor", theClass);
    }
    constructor.setAccessible(true);
    return constructor;
  }

  private static Benchmark createBenchmarkInstance(Constructor<? extends Benchmark> c)
      throws UserCodeException {
    try {
      return c.newInstance();
    } catch (IllegalAccessException impossible) {
      throw new AssertionError(impossible);
    } catch (InstantiationException impossible) {
      throw new AssertionError(impossible);
    } catch (InvocationTargetException e) {
      throw new UserCodeException("Exception thrown from benchmark constructor", e.getCause());
    }
  }

  void validateParameters(ImmutableSetMultimap<String, String> parameters)
      throws InvalidCommandException {
    for (String paramName : parameters.keySet()) {
      Parameter parameter = userParameters.get(paramName);
      if (parameter == null) {
        throw new InvalidCommandException("unrecognized parameter: " + paramName);
      }
      try {
        parameter.validate(parameters.get(paramName));
      } catch (InvalidBenchmarkException e) {
        // TODO(kevinb): this is weird.
        throw new InvalidCommandException(e.getMessage());
      }
    }
  }
}
