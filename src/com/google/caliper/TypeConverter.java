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

import java.lang.reflect.Type;

/**
 * Convert objects to and from Strings.
 */
public class TypeConverter {

  public Object fromString(String value, Type type) {
    if (type instanceof Class) {
      Class<?> c = (Class<?>) type;
      if (c.isEnum()) {
        return Enum.valueOf((Class) c, value);
      } else if (type == Double.class || type == double.class) {
        return Double.valueOf(value);
      } else if (type == Integer.class || type == int.class) {
        return Integer.valueOf(value);
      }
    }
    throw new UnsupportedOperationException(
        "Cannot convert " + value + " of type " + type);
  }

  public String toString(Object value, Type type) {
    if (type instanceof Class) {
      Class<?> c = (Class<?>) type;
      if (c.isEnum()) {
        return value.toString();
      } else if (type == Double.class || type == double.class) {
        return value.toString();
      } else if (type == Integer.class || type == int.class) {
        return value.toString();
      }
    }
    throw new UnsupportedOperationException(
        "Cannot convert " + value + " of type " + type);
  }
}
