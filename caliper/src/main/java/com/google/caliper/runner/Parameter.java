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

import static com.google.common.collect.Iterables.isEmpty;

import com.google.caliper.Param;
import com.google.caliper.api.Benchmark;
import com.google.caliper.api.VmParam;
import com.google.caliper.util.Parser;
import com.google.caliper.util.Parsers;
import com.google.caliper.util.TypedField;
import com.google.caliper.util.Util;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.text.ParseException;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;

/**
 * Represents an injectable parameter, marked with one of @Param, @VmParam. Has nothing to do with
 * particular choices of <i>values</i> for this parameter (except that it knows how to find the
 * <i>default</i> values).
 *
 * @param <T> the injectable type of the parameter (always String if this is a VM parameter)
 */
public final class Parameter<T> {
  public static <T> Parameter<T> create(Field field, Class<T> type)
      throws InvalidBenchmarkException {
    return new Parameter<T>(field, type);
  }

  private final TypedField<Benchmark, T> typedField;
  private final Parser<T> parser;
  private final ImmutableList<T> defaults;

  public Parameter(Field field, Class<T> type) throws InvalidBenchmarkException {
    this.typedField = createTypedField(field, type);

    if (RESERVED_NAMES.contains(typedField.name())) {
      throw new InvalidBenchmarkException("Class '%s' uses reserved parameter name '%s'",
          typedField.containingType(), typedField.name());
    }

    try {
      // TODO(kevinb): perhaps have custom converters listed in .caliperrc or something.
      this.parser = Parsers.byConventionParser(type);
    } catch (IllegalArgumentException e) {
      throw new InvalidBenchmarkException(
          "Type '%s' of parameter field '%s' has no static 'fromString(String)' or "
              + "'valueOf(String)' method", typedField.fieldType(), typedField.name());
    }

    Iterable<T> iterable = DefaultsFinder.FIRST_SUCCESSFUL.findDefaults(typedField, parser);
    this.defaults = ImmutableList.copyOf(iterable);
  }

  static final ImmutableSet<String> RESERVED_NAMES =
      ImmutableSet.of("benchmark", "environment", "run", "trial", "vm");

  // Fake the first type parameter as "Benchmark" just so we don't have to double-parameterize
  // this class.
  @SuppressWarnings("unchecked")
  private TypedField<Benchmark, T> createTypedField(Field field, Class<T> type) {
    return (TypedField<Benchmark, T>) TypedField.from(field, field.getDeclaringClass(), type);
  }

  Parser parser() {
    return parser;
  }

  String name() {
    return typedField.name();
  }

  ImmutableList<T> defaults() {
    return defaults;
  }
  
  void inject(Benchmark benchmark, T value) {
    typedField.setValueOn(benchmark, value);
  }

  private static Object invokeDefaultsMethod(Method defaultsMethod) throws UserCodeException {
    defaultsMethod.setAccessible(true);
    try {
      return defaultsMethod.invoke(null);
    } catch (IllegalAccessException e) {
      throw new AssertionError(e);
    } catch (InvocationTargetException e) {
      throw new UserCodeException(e.getCause());
    }
  }

  private static Object getDefaultsField(Field staticField) {
    staticField.setAccessible(true);
    try {
      return staticField.get(null);
    } catch (IllegalAccessException e) {
      throw new AssertionError(e);
    }
  }

  /** A strategy for choosing default values for parameters. */
  abstract static class DefaultsFinder {
    // Return empty, not null, to say "I didn't find any"
    abstract <T> Iterable<T> findDefaults(TypedField<?, T> field, Parser<T> parser)
        throws InvalidBenchmarkException;

    /*
     * TODO(kevinb): except for the "AllPossible", we'd like *exactly* one to succeed, and would
     * prefer to error out if multiple match, so there's no confusion over precedence.
     */
    static final DefaultsFinder FIRST_SUCCESSFUL = new FirstSuccessful();

    static class FirstSuccessful extends DefaultsFinder {
      static final ImmutableList<DefaultsFinder> ALL = ImmutableList.of(
          new FromAnnotation(),
          new FromValuesMethod(),
          new FromValuesConstant(),
          new AllPossible());

      @Override <T> Iterable<T> findDefaults(TypedField<?, T> field, Parser<T> parser)
          throws InvalidBenchmarkException {
        for (DefaultsFinder defaultsFinder : ALL) {
          Iterable<T> defaults = defaultsFinder.findDefaults(field, parser);
          if (!isEmpty(defaults)) {
            return ImmutableList.copyOf(defaults);
          }
        }
        return ImmutableList.of(); // failure
      }
    }

    static class FromAnnotation extends DefaultsFinder {
      @Override <T> Iterable<T> findDefaults(TypedField<?, T> field, Parser<T> parser)
          throws InvalidBenchmarkException {
        List<T> list = Lists.newArrayList();

        Param annotation = field.getAnnotation(Param.class);
        String[] defaultsAsStrings = (field.getAnnotation(Param.class) != null)
            ? field.getAnnotation(Param.class).value()
            : field.getAnnotation(VmParam.class).value();

        for (String defaultAsString : defaultsAsStrings) {
          try {
            list.add(parser.parse(defaultAsString));
          } catch (ParseException e) {
            throw new InvalidBenchmarkException(
                "Cannot convert default value '%s' to type '%s': %s",
                defaultAsString, field.fieldType(), e.getMessage());
          }
        }
        return list;
      }
    }

    static class FromValuesMethod extends DefaultsFinder {
      @Override <T> Iterable<T> findDefaults(TypedField<?, T> field, Parser<T> parser)
          throws InvalidBenchmarkException {
        String valuesMethodName = field.name() + "Values";
        Class<?> benchmarkClass = field.containingType();
        Method valuesMethod;
        try {
          valuesMethod = benchmarkClass.getDeclaredMethod(valuesMethodName);
        } catch (NoSuchMethodException e) {
          return ImmutableSet.of();
        }

        if (!Modifier.isStatic(valuesMethod.getModifiers())) {
          throw new InvalidBenchmarkException(
              "Default-values method '%s' is not static", valuesMethod.getName());
        }
        Object result = invokeDefaultsMethod(valuesMethod);
        ImmutableList<T> list = copyAndCheck(field.fieldType(), result);
        if (list.isEmpty()) {
          throw new InvalidBenchmarkException(
              "Default-values method '%s' returned no values", valuesMethod.getName());
        }
        return list;
      }
    }

    // TODO(kevinb): eliminate massive duplication between above and below

    static class FromValuesConstant extends DefaultsFinder {
      @Override <T> Iterable<T> findDefaults(TypedField<?, T> field, Parser<T> parser)
          throws InvalidBenchmarkException {
        String valuesFieldName = field.name() + "Values";
        Class<?> benchmarkClass = field.containingType();
        Field valuesField;
        try {
          valuesField = benchmarkClass.getDeclaredField(valuesFieldName);
        } catch (NoSuchFieldException e) {
          return ImmutableSet.of();
        }

        if (!Modifier.isStatic(valuesField.getModifiers())) {
          throw new InvalidBenchmarkException(
              "Default-values field '%s' is not static", valuesField.getName());
        }
        Object result = getDefaultsField(valuesField);
        ImmutableList<T> list = copyAndCheck(field.fieldType(), result);
        if (list.isEmpty()) {
          throw new InvalidBenchmarkException(
              "Default-values field '%s' has no values", valuesField.getName());
        }
        return list;
      }
    }

    static class AllPossible extends DefaultsFinder {
      @SuppressWarnings("unchecked") // protected by 'type' checks
      @Override <T> Iterable<T> findDefaults(TypedField<?, T> field, Parser<T> parser) {
        Class<T> type = field.fieldType();
        if (type == boolean.class || type == Boolean.class) {
          // T == boolean
          return (Iterable<T>) Arrays.asList(true, false);
        }
        if (type.isEnum()) {
          return (Iterable<T>) EnumSet.allOf(type.asSubclass(Enum.class));
        }
        return ImmutableSet.of();
      }
    }
  }

  private static <T> ImmutableList<T> copyAndCheck(Class<T> fieldType, Object result)
      throws InvalidBenchmarkException {
    try {
      ImmutableList<Object> copy = ImmutableList.copyOf((Iterable<?>) result);
      return Util.checkedCast(copy, fieldType);

    } catch (ClassCastException e) {
      throw new InvalidBenchmarkException(
          "Default values must be of type Iterable<%s> (or any subtype)", fieldType);
    }
  }
}
