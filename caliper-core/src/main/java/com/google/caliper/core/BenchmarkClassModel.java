/*
 * Copyright (C) 2018 Google Inc.
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

package com.google.caliper.core;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

import com.google.auto.value.AutoValue;
import com.google.caliper.Param;
import com.google.caliper.api.VmOptions;
import com.google.caliper.util.InvalidCommandException;
import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableSetMultimap;
import com.google.common.collect.Ordering;
import com.google.common.collect.SetMultimap;
import com.google.common.collect.Sets;
import java.io.Serializable;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

/**
 * A simplified model of a benchmark class, containing no reflective references to the class and
 * only those things that the runner needs to know about the class in order to determine the
 * scenarios to be run.
 *
 * @author Colin Decker
 */
@AutoValue
public abstract class BenchmarkClassModel implements Serializable {
  // TODO(b/64154339): Don't use Java serialization for this
  private static final long serialVersionUID = 1;

  /**
   * Creates a model of the given benchmark class and does some validation of it and the given
   * user-provided values for the class' parameter fields.
   */
  public static BenchmarkClassModel create(Class<?> clazz) {
    if (!clazz.getSuperclass().equals(Object.class)) {
      throw new InvalidBenchmarkException(
          "Class '%s' must not extend any class other than %s. Prefer composition.",
          clazz, Object.class);
    }

    if (Modifier.isAbstract(clazz.getModifiers())) {
      throw new InvalidBenchmarkException("Class '%s' is abstract", clazz);
    }

    BenchmarkClassModel.Builder builder =
        new AutoValue_BenchmarkClassModel.Builder()
            .setName(clazz.getName())
            .setSimpleName(clazz.getSimpleName());
    for (Method method : clazz.getDeclaredMethods()) {
      builder.methodsBuilder().add(MethodModel.of(method));
    }
    for (Field field : clazz.getDeclaredFields()) {
      if (field.isAnnotationPresent(Param.class)) {
        builder.parametersBuilder().put(field.getName(), Parameters.validateAndGetDefaults(field));
      }
    }
    VmOptions vmOptions = clazz.getAnnotation(VmOptions.class);
    if (vmOptions != null) {
      builder.vmOptionsBuilder().add(vmOptions.value());
    }
    return builder.build();
  }

  /**
   * Validates the given user-provided parameters against the parameter fields on the benchmark
   * class.
   */
  public static void validateUserParameters(
      Class<?> clazz, SetMultimap<String, String> userParameters) {
    for (String paramName : userParameters.keySet()) {
      try {
        Field field = clazz.getDeclaredField(paramName);
        Parameters.validate(field, userParameters.get(paramName));
      } catch (NoSuchFieldException e) {
        throw new InvalidCommandException("unrecognized parameter: " + paramName);
      } catch (InvalidBenchmarkException e) {
        // TODO(kevinb): this is weird.
        throw new InvalidCommandException(e.getMessage());
      }
    }
  }

  /** Returns the fully qualified name of the benchmark class. */
  public abstract String name();

  /** Returns the simple name of the benchmark class. */
  public abstract String simpleName();

  /** Returns the set of all methods declared on the class. */
  public abstract ImmutableSet<MethodModel> methods();

  /**
   * Returns the set of parameter fields for the class and their default values as a map from field
   * name to set of default values.
   */
  // NOTE: Not a multimap because parameters with no default values are legal, and multimap can't
  // include a key mapped to nothing.
  abstract ImmutableMap<String, ImmutableSet<String>> parameters();

  /**
   * Returns a multimap containing the full set of parameter values to use for the benchmark. For
   * parameters on the benchmark that have values in the given user-supplied parameters, the user's
   * specified values are used. For all other parameters, the default values specified in the
   * annotation or implied by the type are used.
   *
   * @throws IllegalArgumentException if a parameter for the benchmark has neither user-specified
   *     values nor default values
   */
  public final ImmutableSetMultimap<String, String> fillInDefaultParameterValues(
      ImmutableSetMultimap<String, String> userParameters) {
    ImmutableSetMultimap.Builder<String, String> combined = ImmutableSetMultimap.builder();

    // For user parameters, this'll actually be the same as parameters().keySet(), since any extras
    // given at the command line are treated as errors; for VM parameters this is not the case.
    for (String name : Sets.union(parameters().keySet(), userParameters.keySet())) {
      ImmutableSet<String> values =
          userParameters.containsKey(name) ? userParameters.get(name) : parameters().get(name);
      combined.putAll(name, values);
      checkArgument(!values.isEmpty(), "ERROR: No default value provided for %s", name);
    }
    return combined.orderKeysBy(Ordering.natural()).build();
  }

  /** Returns the set of VM options specified with the {@code @VmOptions} annotation. */
  public abstract ImmutableSet<String> vmOptions();

  @Override
  public String toString() {
    return name();
  }

  /** Builder for {@link BenchmarkClassModel} instances. */
  @AutoValue.Builder
  public abstract static class Builder {

    /** Sets the name of the benchmark class. */
    abstract Builder setName(String className);

    /** Sets the simple name of the benchmark class. */
    abstract Builder setSimpleName(String simpleName);

    /** Returns a builder for adding to the set of methods on the class. */
    abstract ImmutableSet.Builder<MethodModel> methodsBuilder();

    /** Returns a builder for adding to the map of parameters for the class. */
    public abstract ImmutableMap.Builder<String, ImmutableSet<String>> parametersBuilder();

    /** Sets the VM options to use for benchmarking the class. */
    public abstract Builder setVmOptions(Iterable<String> vmOptions);

    /** Returns a builder for adding to the set of VM options for the class. */
    // NOTE: This also makes explicitly setting VM options optional, which is useful for tests.
    public abstract ImmutableSet.Builder<String> vmOptionsBuilder();

    /** Returns a new {@link BenchmarkClassModel} with all the previously set values. */
    public abstract BenchmarkClassModel build();
  }

  /** Model of a method on a benchmark class. */
  @AutoValue
  public abstract static class MethodModel implements Serializable {

    /** Creates a new {@link MethodModel} representing the given method. */
    public static MethodModel of(Method method) {
      MethodModel.Builder builder =
          new AutoValue_BenchmarkClassModel_MethodModel.Builder()
              .setName(method.getName())
              .setModifiers(method.getModifiers())
              .setDeclaringClass(method.getDeclaringClass().getName());
      if (!method.getReturnType().equals(void.class)) {
        builder.setReturnType(method.getReturnType().getName());
      }

      for (Class<?> parameterType : method.getParameterTypes()) {
        builder.parameterTypesBuilder().add(parameterType.getName());
      }
      for (Class<?> exceptionType : method.getExceptionTypes()) {
        builder.exceptionTypesBuilder().add(exceptionType.getName());
      }
      for (Annotation annotation : method.getAnnotations()) {
        builder.annotationTypesBuilder().add(annotation.annotationType().getName());
      }

      return builder.build();
    }

    /** Returns the name of this method. */
    public abstract String name();

    /** Returns the modifiers for this method. */
    public abstract int modifiers();

    /** Returns the class that declared this method. */
    public abstract String declaringClass();

    /** Returns the method's return type, or absent for a {@code void} method. */
    public abstract Optional<String> returnType();

    /** Returns the method's parameter types. */
    public abstract ImmutableList<String> parameterTypes();

    /** Returns the types of exceptions the method declares that it may throw. */
    public abstract ImmutableSet<String> exceptionTypes();

    /** Returns the types of annotations that are present on the method. */
    public abstract ImmutableSet<String> annotationTypes();

    /**
     * Returns whether or not the set of {@link #annotationTypes()} contains an annotation with the
     * given type name.
     */
    public final boolean isAnnotationPresent(String annotationTypeName) {
      checkNotNull(annotationTypeName);
      return annotationTypes().contains(annotationTypeName);
    }

    /**
     * Returns whether or not the set of {@link #annotationTypes()} contains an annotation of the
     * given type.
     */
    public final boolean isAnnotationPresent(Class<?> annotationType) {
      return isAnnotationPresent(annotationType.getName());
    }

    /** Builder for {@link MethodModel} instances. */
    @AutoValue.Builder
    interface Builder {
      /** Sets the name of this method. */
      Builder setName(String name);

      /** Sets the modifiers for this method. */
      Builder setModifiers(int modifiers);

      /** Sets the class that declared this method. */
      Builder setDeclaringClass(String declaringClass);

      /** Sets the method's return type. */
      Builder setReturnType(String returnType);

      /** Returns a builder for adding the method's parameter types. */
      ImmutableList.Builder<String> parameterTypesBuilder();

      /**
       * Returns a builder for adding the types of exceptions the method declares that it may throw.
       */
      ImmutableSet.Builder<String> exceptionTypesBuilder();

      /** Returns a builder for adding the types of annotations that are present on the method. */
      ImmutableSet.Builder<String> annotationTypesBuilder();

      /** Builds the {@link MethodModel}. */
      MethodModel build();
    }
  }
}
