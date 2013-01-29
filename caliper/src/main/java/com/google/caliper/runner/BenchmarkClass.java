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

import com.google.caliper.Benchmark;
import com.google.caliper.Param;
import com.google.caliper.api.SkipThisScenarioException;
import com.google.caliper.api.VmOptions;
import com.google.caliper.util.InvalidCommandException;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableSetMultimap;
import com.google.common.collect.ImmutableSortedMap;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

/**
 * An instance of this type represents a user-provided class that extends Benchmark. It manages
 * creating, setting up and destroying instances of that class.
 */
public final class BenchmarkClass {
  private final Class<? extends Benchmark> theClass;
  private final Constructor<? extends Benchmark> constructor;
  private final ParameterSet userParameters;
  private final ImmutableSet<String> benchmarkFlags;

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

    this.benchmarkFlags = getVmOptions(theClass);
  }

  public ParameterSet userParameters() {
    return userParameters;
  }

  public ImmutableSet<String> vmOptions() {
    return benchmarkFlags;
  }

  // TODO: perhaps move this to Instrument and let instruments override it?

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

  public Benchmark createAndStage(ImmutableSortedMap<String, String> userParameterValues)
      throws UserCodeException {
    Benchmark benchmark = createBenchmarkInstance(constructor);
    userParameters.injectAll(benchmark, userParameterValues);

    boolean setupSuccess = false;
    try {
      callSetUp(benchmark);
      setupSuccess = true;
    } finally {
      // If setUp fails, we should call tearDown. If this method throws an exception, we
      // need to call tearDown from here, because no one else has the reference to the
      // Benchmark.
      if (!setupSuccess) {
        callTearDown(benchmark);
      }
    }
    return benchmark;
  }

  public void cleanup(Benchmark benchmark) throws UserCodeException {
    callTearDown(benchmark);
  }

  @VisibleForTesting Class<? extends Benchmark> benchmarkClass() {
    return theClass;
  }

  public String name() {
    return theClass.getName();
  }

  @Override public String toString() {
    return name();
  }

  private static final Method SETUP_METHOD = findBenchmarkMethod("setUp");

  private static final Method TEARDOWN_METHOD = findBenchmarkMethod("tearDown");

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

  private void callTearDown(Benchmark benchmark) throws UserCodeException {
    try {
      TEARDOWN_METHOD.invoke(benchmark);
    } catch (IllegalAccessException e) {
      throw new AssertionError(e);
    } catch (InvocationTargetException e) {
      throw new UserCodeException("Exception thrown during tearDown", e.getCause());
    }
  }

  private static Method findBenchmarkMethod(String methodName) {
    try {
      Method method = Benchmark.class.getDeclaredMethod(methodName);
      method.setAccessible(true);
      return method;
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

  private static ImmutableSet<String> getVmOptions(Class<? extends Benchmark> benchmarkClass) {
    VmOptions annotation = benchmarkClass.getAnnotation(VmOptions.class);
    return (annotation == null)
        ? ImmutableSet.<String>of()
        : ImmutableSet.copyOf(annotation.value());
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
