/**
 * Copyright (C) 2009 Google Inc.
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

package com.google.caliper;

import com.google.common.collect.ImmutableMap;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.Map;

/**
 * Convert objects to and from Strings.
 */
final class TypeConverter {
  private TypeConverter() {}

  public static Object fromString(String value, Type type) {
    if (type == String.class) {
      return value;
    }

    Class<?> c = wrap((Class<?>) type);
    try {
      Method m = c.getMethod("valueOf", String.class);
      return m.invoke(null, value);
    } catch (Exception e) {
      throw new UnsupportedOperationException(
          "Cannot convert " + value + " of type " + type, e);
    }
  }

  // safe because both Long.class and long.class are of type Class<Long>
  @SuppressWarnings("unchecked")
  private static <T> Class<T> wrap(Class<T> c) {
    return c.isPrimitive() ? (Class<T>) PRIMITIVES_TO_WRAPPERS.get(c) : c;
  }

  private static final Map<Class<?>, Class<?>> PRIMITIVES_TO_WRAPPERS
    = new ImmutableMap.Builder<Class<?>, Class<?>>()
      .put(boolean.class, Boolean.class)
      .put(byte.class, Byte.class)
      .put(char.class, Character.class)
      .put(double.class, Double.class)
      .put(float.class, Float.class)
      .put(int.class, Integer.class)
      .put(long.class, Long.class)
      .put(short.class, Short.class)
      .put(void.class, Void.class)
      .build();
}
