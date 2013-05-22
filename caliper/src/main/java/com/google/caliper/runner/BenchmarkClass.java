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

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Throwables.propagateIfInstanceOf;

import com.google.caliper.Benchmark;
import com.google.caliper.Param;
import com.google.caliper.api.SkipThisScenarioException;
import com.google.caliper.api.VmOptions;
import com.google.caliper.util.InvalidCommandException;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Function;
import com.google.common.base.Objects;
import com.google.common.base.Optional;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableSetMultimap;
import com.google.common.collect.ImmutableSortedMap;
import com.google.common.collect.ImmutableSortedSet;
import com.google.common.collect.Ordering;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

/**
 * An instance of this type represents a user-provided class that extends Benchmark. It manages
 * creating, setting up and destroying instances of that class.
 */
final class BenchmarkClass {
  private final Class<?> theClass;
  private final Constructor<?> constructor;
  private final ParameterSet userParameters;
  private final ImmutableSet<String> benchmarkFlags;

  public BenchmarkClass(Class<?> theClass) throws InvalidBenchmarkException {
    this.theClass = checkNotNull(theClass);

    if (Modifier.isAbstract(theClass.getModifiers())) {
      throw new InvalidBenchmarkException("Class '%s' is abstract", theClass);
    }

    // TODO: check for nested, non-static classes (non-abstract, but no constructor?)
    // this will fail later anyway (no way to declare parameterless nested constr., but
    // maybe signal this better?

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

  public ImmutableSortedSet<BenchmarkMethod> findAllBenchmarkMethods(
      Instrument instrument) throws InvalidBenchmarkException {
    ImmutableSortedSet.Builder<BenchmarkMethod> result = ImmutableSortedSet.orderedBy(
        Ordering.natural().onResultOf(new Function<BenchmarkMethod, String>() {
          @Override public String apply(BenchmarkMethod method) {
            return method.method().getName();
          }
        }));
    for (Method method : theClass.getDeclaredMethods()) {
      if (instrument.isBenchmarkMethod(method)) {
        result.add(instrument.createBenchmarkMethod(this, method));
      }
    }
    return result.build();
  }

  public Object createAndStage(ImmutableSortedMap<String, String> userParameterValues)
      throws UserCodeException {
    Object benchmark = createBenchmarkInstance(constructor);
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

  public void cleanup(Object benchmark) throws UserCodeException {
    callTearDown(benchmark);
  }

  @VisibleForTesting Class<?> benchmarkClass() {
    return theClass;
  }

  public String name() {
    return theClass.getName();
  }

  @Override public boolean equals(Object obj) {
    if (obj == this) {
      return true;
    } else if (obj instanceof BenchmarkClass) {
      BenchmarkClass that = (BenchmarkClass) obj;
      return this.theClass.equals(that.theClass);
    } else {
      return false;
    }
  }

  @Override public int hashCode() {
    return Objects.hashCode(theClass);
  }

  @Override public String toString() {
    return name();
  }

  // We have to do this reflectively because it'd be too much of a pain to make setUp public
  private void callSetUp(Object benchmark) throws UserCodeException {
    try {
      Optional<Method> method = findBenchmarkMethod("setUp");
      if (method.isPresent()) {
        method.get().invoke(benchmark);
      }
    } catch (IllegalAccessException e) {
      throw new AssertionError(e);
    } catch (InvocationTargetException e) {
      propagateIfInstanceOf(e.getCause(), SkipThisScenarioException.class);
      throw new UserCodeException("Exception thrown during setUp", e.getCause());
    }
  }

  private void callTearDown(Object benchmark) throws UserCodeException {
    try {
      Optional<Method> method = findBenchmarkMethod("tearDown");
      if (method.isPresent()) {
        method.get().invoke(benchmark);
      }
    } catch (IllegalAccessException e) {
      throw new AssertionError(e);
    } catch (InvocationTargetException e) {
      throw new UserCodeException("Exception thrown during tearDown", e.getCause());
    }
  }

  private static Optional<Method> findBenchmarkMethod(String methodName) {
    try {
      Method method = Benchmark.class.getDeclaredMethod(methodName);
      method.setAccessible(true);
      return Optional.of(method);
    } catch (NoSuchMethodException e) {
      return Optional.absent();
    }
  }

  private static Constructor<?> findConstructor(Class<?> theClass)
      throws InvalidBenchmarkException {
    Constructor<?> constructor;
    try {
      constructor = theClass.getDeclaredConstructor();
    } catch (NoSuchMethodException e) {
      throw new InvalidBenchmarkException(
          "Class '%s' has no parameterless constructor", theClass);
    }
    constructor.setAccessible(true);
    return constructor;
  }

  private static ImmutableSet<String> getVmOptions(Class<?> benchmarkClass) {
    VmOptions annotation = benchmarkClass.getAnnotation(VmOptions.class);
    return (annotation == null)
        ? ImmutableSet.<String>of()
        : ImmutableSet.copyOf(annotation.value());
  }

  private static Object createBenchmarkInstance(Constructor<?> c) throws UserCodeException {
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
