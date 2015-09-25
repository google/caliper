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

import com.google.caliper.AfterExperiment;
import com.google.caliper.BeforeExperiment;
import com.google.caliper.Param;
import com.google.caliper.api.SkipThisScenarioException;
import com.google.caliper.api.VmOptions;
import com.google.caliper.util.InvalidCommandException;
import com.google.caliper.util.Reflection;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Objects;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableSetMultimap;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * An instance of this type represents a user-provided class. It manages creating, setting up and
 * destroying instances of that class.
 */
final class BenchmarkClass {
  private static final Logger logger = Logger.getLogger(BenchmarkClass.class.getName());

  static BenchmarkClass forClass(Class<?> theClass) throws InvalidBenchmarkException {
    return new BenchmarkClass(theClass);
  }

  final Class<?> theClass;
  private final ParameterSet userParameters;
  private final ImmutableSet<String> benchmarkFlags;

  private BenchmarkClass(Class<?> theClass) throws InvalidBenchmarkException {
    this.theClass = checkNotNull(theClass);

    if (!theClass.getSuperclass().equals(Object.class)) {
      throw new InvalidBenchmarkException(
          "%s must not extend any class other than %s. Prefer composition.",
          theClass, Object.class);
    }

    if (Modifier.isAbstract(theClass.getModifiers())) {
      throw new InvalidBenchmarkException("Class '%s' is abstract", theClass);
    }

    // TODO: check for nested, non-static classes (non-abstract, but no constructor?)
    // this will fail later anyway (no way to declare parameterless nested constr., but
    // maybe signal this better?

    this.userParameters = ParameterSet.create(theClass, Param.class);

    this.benchmarkFlags = getVmOptions(theClass);
  }

  ImmutableSet<Method> beforeExperimentMethods() {
    return Reflection.getAnnotatedMethods(theClass, BeforeExperiment.class);
  }

  ImmutableSet<Method> afterExperimentMethods() {
    return Reflection.getAnnotatedMethods(theClass, AfterExperiment.class);
  }

  public ParameterSet userParameters() {
    return userParameters;
  }

  public ImmutableSet<String> vmOptions() {
    return benchmarkFlags;
  }

  // TODO(gak): use these methods in the worker as well
  public void setUpBenchmark(Object benchmarkInstance) throws UserCodeException {
    boolean setupSuccess = false;
    try {
      callSetUp(benchmarkInstance);
      setupSuccess = true;
    } finally {
      // If setUp fails, we should call tearDown. If this method throws an exception, we
      // need to call tearDown from here, because no one else has the reference to the
      // Benchmark.
      if (!setupSuccess) {
        try {
          callTearDown(benchmarkInstance);
        } catch (UserCodeException e) {
          // The exception thrown during setUp shouldn't be lost, as it's probably more
          // important to the user.
          logger.log(
              Level.INFO,
              "in @AfterExperiment methods called because @BeforeExperiment methods failed",
              e);
        }
      }
    }
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

  private void callSetUp(Object benchmark) throws UserCodeException {
    for (Method method : beforeExperimentMethods()) {
      try {
        method.invoke(benchmark);
      } catch (IllegalAccessException e) {
        throw new AssertionError(e);
      } catch (InvocationTargetException e) {
        propagateIfInstanceOf(e.getCause(), SkipThisScenarioException.class);
        throw new UserCodeException(
            "Exception thrown from a @BeforeExperiment method", e.getCause());
      }
    }
  }

  private void callTearDown(Object benchmark) throws UserCodeException {
    for (Method method : afterExperimentMethods()) {
      try {
        method.invoke(benchmark);
      } catch (IllegalAccessException e) {
        throw new AssertionError(e);
      } catch (InvocationTargetException e) {
        propagateIfInstanceOf(e.getCause(), SkipThisScenarioException.class);
        throw new UserCodeException(
            "Exception thrown from an @AfterExperiment method", e.getCause());
      }
    }
  }

  private static ImmutableSet<String> getVmOptions(Class<?> benchmarkClass) {
    VmOptions annotation = benchmarkClass.getAnnotation(VmOptions.class);
    return (annotation == null)
        ? ImmutableSet.<String>of()
        : ImmutableSet.copyOf(annotation.value());
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
