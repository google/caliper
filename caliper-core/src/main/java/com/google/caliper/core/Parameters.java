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

package com.google.caliper.core;

import com.google.caliper.Param;
import com.google.caliper.util.Parser;
import com.google.caliper.util.Parsers;
import com.google.caliper.util.Util;
import com.google.common.collect.ImmutableSet;
import com.google.common.primitives.Primitives;
import java.lang.reflect.Field;
import java.text.ParseException;

/** Utilities for dealing with {@code @Param} fields. */
final class Parameters {

  private Parameters() {}

  /** Validates the given parameter field and returns its default values. */
  static ImmutableSet<String> validateAndGetDefaults(Field field) {
    if (Util.isStatic(field)) {
      throw new InvalidBenchmarkException(
          "Parameter field '%s' must not be static", field.getName());
    }
    if (RESERVED_NAMES.contains(field.getName())) {
      throw new InvalidBenchmarkException(
          "Class '%s' uses reserved parameter name '%s'",
          field.getDeclaringClass(), field.getName());
    }

    field.setAccessible(true);

    ImmutableSet<String> defaults = findDefaults(field);
    validate(field, defaults);
    return defaults;
  }

  private static Parser<?> getParser(Field field) {
    Class<?> type = Primitives.wrap(field.getType());
    try {
      return Parsers.conventionalParser(type);
    } catch (NoSuchMethodException e) {
      throw new InvalidBenchmarkException(
          "Type '%s' of parameter field '%s' has no recognized "
              + "String-converting method; see <TODO> for details",
          type.getName(), field.getName());
    }
  }

  /** Validates the given values can be parsed and assigned to the given feild. */
  static void validate(Field field, Iterable<String> values) {
    Parser<?> parser = getParser(field);
    for (String value : values) {
      try {
        parser.parse(value);
      } catch (ParseException e) {
        throw new InvalidBenchmarkException(
            "Cannot convert value '%s' to type '%s': %s", value, field.getType(), e.getMessage());
      }
    }
  }

  static final ImmutableSet<String> RESERVED_NAMES =
      ImmutableSet.of(
          "benchmark",
          "environment",
          "instrument",
          "measurement", // e.g. runtime, allocation, etc.
          "run",
          "trial", // currently unused, but we might need it
          "vm");

  private static ImmutableSet<String> findDefaults(Field field) {
    String[] defaultsAsStrings = field.getAnnotation(Param.class).value();
    if (defaultsAsStrings.length > 0) {
      return ImmutableSet.copyOf(defaultsAsStrings);
    }

    Class<?> type = field.getType();
    if (type == boolean.class) {
      return ALL_BOOLEANS;
    }

    if (type.isEnum()) {
      ImmutableSet.Builder<String> builder = ImmutableSet.builder();
      for (Object enumConstant : type.getEnumConstants()) {
        builder.add(enumConstant.toString());
      }
      return builder.build();
    }
    return ImmutableSet.of();
  }

  private static final ImmutableSet<String> ALL_BOOLEANS = ImmutableSet.of("true", "false");
}
