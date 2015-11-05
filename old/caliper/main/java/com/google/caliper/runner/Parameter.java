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

import com.google.caliper.Param;
import com.google.caliper.util.Parser;
import com.google.caliper.util.Parsers;
import com.google.caliper.util.Util;
import com.google.common.collect.ImmutableCollection;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.primitives.Primitives;

import java.lang.reflect.Field;
import java.text.ParseException;

/**
 * Represents an injectable parameter, marked with one of @Param, @VmParam. Has nothing to do with
 * particular choices of <i>values</i> for this parameter (except that it knows how to find the
 * <i>default</i> values).
 */
public final class Parameter {
  public static Parameter create(Field field) throws InvalidBenchmarkException {
    return new Parameter(field);
  }

  private final Field field;
  private final Parser<?> parser;
  private final ImmutableList<String> defaults;

  public Parameter(Field field) throws InvalidBenchmarkException {
    if (Util.isStatic(field)) {
      throw new InvalidBenchmarkException("Parameter field '%s' must not be static",
          field.getName());
    }
    if (RESERVED_NAMES.contains(field.getName())) {
      throw new InvalidBenchmarkException("Class '%s' uses reserved parameter name '%s'",
          field.getDeclaringClass(), field.getName());
    }

    this.field = field;
    field.setAccessible(true);

    Class<?> type = Primitives.wrap(field.getType());
    try {
      this.parser = Parsers.conventionalParser(type);
    } catch (NoSuchMethodException e) {
      throw new InvalidBenchmarkException("Type '%s' of parameter field '%s' has no recognized "
          + "String-converting method; see <TODO> for details", type, field.getName());
    }

    this.defaults = findDefaults(field);
    validate(defaults);
  }

  void validate(ImmutableCollection<String> values) throws InvalidBenchmarkException {
    for (String valueAsString : values) {
      try {
        parser.parse(valueAsString);
      } catch (ParseException e) {
        throw new InvalidBenchmarkException(
            "Cannot convert value '%s' to type '%s': %s",
            valueAsString, field.getType(), e.getMessage());
      }
    }
  }

  static final ImmutableSet<String> RESERVED_NAMES = ImmutableSet.of(
      "benchmark",
      "environment",
      "instrument",
      "measurement", // e.g. runtime, allocation, etc.
      "run",
      "trial", // currently unused, but we might need it
      "vm");

  String name() {
    return field.getName();
  }

  ImmutableList<String> defaults() {
    return defaults;
  }

  void inject(Object benchmark, String value) {
    try {
      Object o = parser.parse(value);
      field.set(benchmark, o);
    } catch (ParseException impossible) {
      // already validated both defaults and command-line
      throw new AssertionError(impossible);
    } catch (IllegalAccessException impossible) {
      throw new AssertionError(impossible);
    }
  }

  private static ImmutableList<String> findDefaults(Field field) {
    String[] defaultsAsStrings = field.getAnnotation(Param.class).value();
    if (defaultsAsStrings.length > 0) {
      return ImmutableList.copyOf(defaultsAsStrings);
    }

    Class<?> type = field.getType();
    if (type == boolean.class) {
      return ALL_BOOLEANS;
    }

    if (type.isEnum()) {
      ImmutableList.Builder<String> builder = ImmutableList.builder();
      for (Object enumConstant : type.getEnumConstants()) {
        builder.add(enumConstant.toString());
      }
      return builder.build();
    }
    return ImmutableList.of();
  }

  private static final ImmutableList<String> ALL_BOOLEANS = ImmutableList.of("true", "false");
}
