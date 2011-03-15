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
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.EnumSet;

/**
 * @author Kevin Bourrillion
 */
public class Parameter {
  private final Field field;
  private final ImmutableList<?> defaults;

  private static final ImmutableSet<String> RESERVED_NAMES =
      ImmutableSet.of("environment", "run", "vm", "trial");

  Parameter(Field field) {
    if (isReservedName(field.getName())) {
      throw new InvalidBenchmarkException("Reserved parameter name: " + field.getName());
    }
    field.setAccessible(true);
    this.field = field;
    this.defaults = findDefaults(field);
  }

  private static ImmutableList<?> findDefaults(Field field) {
    for (DefaultsFinder defaultsFinder : DefaultsFinder.ALL) {
      Iterable<?> defaults = defaultsFinder.findDefaults(field);
      if (!isEmpty(defaults)) {
        return ImmutableList.copyOf(defaults);
      }
    }
    return null;
  }

  String name() {
    return field.getName();
  }

  ImmutableList<?> defaults() {
    // Defer this failure, because it's possible defaults won't even be asked for
    if (defaults == null) {
      throw new InvalidBenchmarkException("no defaults found for: " + name());
    }
    return defaults;
  }
  
  public void inject(Benchmark benchmark, Object value) {
    try {
      field.set(benchmark, value);
    } catch (IllegalAccessException e) {
      throw new AssertionError(e);
    }
  }

  static boolean isReservedName(String name) {
    return RESERVED_NAMES.contains(name);
  }

  abstract static class DefaultsFinder {
    abstract Iterable<?> findDefaults(Field field);

    // Use these strategies in order until one yields a nonempty list of defaults, if any
    static final ImmutableList<DefaultsFinder> ALL = ImmutableList.of(
        new FromAnnotation(),
        new FromValuesMethod(),
        new FromValuesConstant(),
        new AllPossible());

    static class FromAnnotation extends DefaultsFinder {
      @Override Iterable<?> findDefaults(Field field) {
        Object[] defaults = field.getAnnotation(Param.class).value();
        return Arrays.asList(defaults);
      }
    }

    static class FromValuesMethod extends DefaultsFinder {
      @Override Iterable<?> findDefaults(Field field) {
        String valuesMethodName = field.getName() + "Values";
        Class<?> benchmarkClass = field.getDeclaringClass();
        try {
          Method valuesMethod = benchmarkClass.getDeclaredMethod(valuesMethodName);
          return toIterable(ReflectionHelper.invokeStatic(valuesMethod));
        } catch (NoSuchMethodException e) {
          return ImmutableSet.of();
        }
      }
    }

    static class FromValuesConstant extends DefaultsFinder {
      @Override Iterable<?> findDefaults(Field field) {
        String valuesFieldName = field.getName() + "Values";
        Class<?> benchmarkClass = field.getDeclaringClass();
        try {
          Field valuesField = benchmarkClass.getDeclaredField(valuesFieldName);
          return toIterable(ReflectionHelper.getStatic(valuesField));
        } catch (NoSuchFieldException e) {
          return ImmutableSet.of();
        }
      }
    }

    static class AllPossible extends DefaultsFinder {
      @Override Iterable<?> findDefaults(Field field) {
        Class<?> type = field.getType();
        if (type == boolean.class) {
          return Arrays.asList(true, false);
        }
        if (type.isEnum()) {
          return EnumSet.allOf(type.asSubclass(Enum.class));
        }
        return ImmutableSet.of();
      }
    }
  }

  private static Iterable<?> toIterable(Object result) {
    if (result instanceof Iterable) {
      return (Iterable<?>) result;
    } else {
      throw new InvalidBenchmarkException("not an iterable"); // TODO
    }
  }
}
