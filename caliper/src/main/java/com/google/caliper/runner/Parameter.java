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

import com.google.caliper.api.Benchmark;
import com.google.caliper.api.Param;
import com.google.caliper.util.Parser;
import com.google.caliper.util.Parsers;
import com.google.caliper.util.TypedField;
import com.google.caliper.util.Util;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;

import java.lang.reflect.Field;
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
public class Parameter<T> {
  public static <T> Parameter<T> create(Field field, Class<T> type)
      throws InvalidBenchmarkException {
    return new Parameter<T>(field, type);
  }

  private final TypedField<Benchmark, T> typedField;
  private final Parser<T> parser;
  private final ImmutableList<T> defaults;

  public Parameter(Field field, Class<T> type) throws InvalidBenchmarkException {
    if (isReservedName(field.getName())) {
      throw new InvalidBenchmarkException("Reserved parameter name: " + field.getName());
    }
    this.typedField = createTypedField(field, type);

    try {
      this.parser = Parsers.byConventionParser(type);
    } catch (IllegalArgumentException e) {
      throw new InvalidBenchmarkException(
          "Type has no suitable fromString or valueOf method: " + typedField);
    }

    Iterable<T> iterable = DefaultsFinder.FIRST_SUCCESSFUL.findDefaults(typedField, parser);
    this.defaults = ImmutableList.copyOf(iterable);
  }

  // Fake the first type parameter as "Benchmark" just so we don't have to double-parameterize
  // this class.
  @SuppressWarnings("unchecked")
  private TypedField<Benchmark, T> createTypedField(Field field, Class<T> type) {
    return (TypedField<Benchmark, T>) TypedField.from(field, field.getDeclaringClass(), type);
  }

  // TODO: -J should check this...
  static final ImmutableSet<String> RESERVED_NAMES =
      ImmutableSet.of("environment", "run", "trial", "vm");

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

  static boolean isReservedName(String name) {
    return RESERVED_NAMES.contains(name);
  }

  /** A strategy for choosing default values for parameters. */
  abstract static class DefaultsFinder {
    abstract <T> Iterable<T> findDefaults(TypedField<?, T> field, Parser<T> parser)
        throws InvalidBenchmarkException;

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
        String[] defaultsAsStrings = field.getAnnotation(Param.class).value();
        for (String defaultAsString : defaultsAsStrings) {
          try {
            list.add(parser.parse(defaultAsString));
          } catch (ParseException e) {
            throw new InvalidBenchmarkException("blah", e); // TODO
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
          valuesMethod = benchmarkClass.getDeclaredMethod(valuesMethodName, String.class);
        } catch (NoSuchMethodException e) {
          return ImmutableSet.of();
        }

        if (!Modifier.isStatic(valuesMethod.getModifiers())) {
          throw new InvalidBenchmarkException("Method expected to be static " + valuesMethod);
        }
        Object result = ReflectionHelper.invokeStatic(valuesMethod);
        return castToIterableOf(field.fieldType(), result);
      }
    }

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
          throw new InvalidBenchmarkException("Field expected to be static " + valuesField);
        }
        Object result = ReflectionHelper.getStatic(valuesField);
        return castToIterableOf(field.fieldType(), result);
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

  private static <T> Iterable<T> castToIterableOf(Class<T> fieldType, Object result)
      throws InvalidBenchmarkException {
    if (result instanceof Iterable) {
      Iterable<?> iterable = (Iterable<?>) result;

      // TODO: IBE > CCE
      return Iterables.transform(iterable, Util.castFunction(fieldType));
    } else {
      throw new InvalidBenchmarkException("not an iterable"); // TODO
    }
  }
}
