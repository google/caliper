/*
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

import java.lang.reflect.Field;
import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.Map;
import java.util.TreeMap;

/**
 * A parameter in a {@link SimpleBenchmark}.
 */
abstract class Parameter<T> {

  private final Field field;

  private Parameter(Field field) {
    this.field = field;
  }

  /**
   * Returns all properties for the given class.
   */
  public static Map<String, Parameter<?>> forClass(Class<? extends Benchmark> suiteClass) {
    Map<String, Parameter<?>> parameters = new TreeMap<String, Parameter<?>>();
    for (final Field field : suiteClass.getDeclaredFields()) {
      if (field.isAnnotationPresent(Param.class)) {
        field.setAccessible(true);
        Parameter parameter = Parameter.forField(suiteClass, field);
        parameters.put(parameter.getName(), parameter);
      }
    }
    return parameters;
  }

  public static Parameter forField(
      Class<? extends Benchmark> suiteClass, final Field field) {
    // First check for String values on the annotation itself
    final Object[] defaults = field.getAnnotation(Param.class).value();
    if (defaults.length > 0) {
      return new Parameter<Object>(field) {
        public Collection<Object> values() throws Exception {
          return Arrays.<Object>asList(defaults);
        }
      };
    }

    Parameter result = null;
    Type returnType = null;
    Member member = null;

    // Now check for a fooValues() method
    try {
      final Method valuesMethod = suiteClass.getDeclaredMethod(field.getName() + "Values");
      valuesMethod.setAccessible(true);
      returnType = field.getGenericType();
      result = new Parameter<Object>(field) {
        @SuppressWarnings("unchecked") // guarded below
        public Collection<Object> values() throws Exception {
          return (Collection<Object>) valuesMethod.invoke(null);
        }
      };
    } catch (NoSuchMethodException ignored) {
    }

    // Now check for a fooValues field
    try {
      final Field valuesField = suiteClass.getDeclaredField(field.getName() + "Values");
      valuesField.setAccessible(true);
      member = valuesField;
      if (result != null) {
        throw new ConfigurationException("Two values members defined for " + field);
      }
      returnType = valuesField.getGenericType();
      result = new Parameter<Object>(field) {
        @SuppressWarnings("unchecked") // guarded below
        public Collection<Object> values() throws Exception {
          return (Collection<Object>) valuesField.get(null);
        }
      };
    } catch (NoSuchFieldException ignored) {
    }

    if (member != null && !Modifier.isStatic(member.getModifiers())) {
        throw new ConfigurationException("Values member must be static " + member);
    }

    // If there isn't a values member but the parameter is an enum, we default
    // to EnumSet.allOf.
    if (member == null && field.getType().isEnum()) {
      returnType = Collection.class;
      result = new Parameter<Object>(field) {
        @SuppressWarnings("unchecked") // guarded above
        public Collection<Object> values() throws Exception {
          return EnumSet.allOf((Class<Enum>) field.getType());
        }
      };
    }

    if (result == null) {
      return new Parameter<Object>(field) {
        @Override public Collection<Object> values() throws Exception {
          return Collections.emptySet();
        }
      };
    } else if (!isValidReturnType(returnType)) {
      throw new ConfigurationException("Invalid return type " + returnType
          + " for values member " + member + "; must be Collection");
    }
    return result;
  }

  private static boolean isValidReturnType(Type returnType) {
    if (returnType == Collection.class) {
      return true;
    }
    if (returnType instanceof ParameterizedType) {
      ParameterizedType type = (ParameterizedType) returnType;
      if (type.getRawType() == Collection.class) {
        return true;
      }
    }
    return false;
  }

  /**
   * Sets the value of this property to the specified value for the given suite.
   */
  public void set(Benchmark suite, Object value) throws Exception {
    field.set(suite, value);
  }

  /**
   * Returns the available values of the property as specified by the suite.
   */
  public abstract Collection<T> values() throws Exception;

  /**
   * Returns the parameter's type, such as double.class.
   */
  public Type getType() {
    return field.getGenericType();
  }

  /**
   * Returns the field's name.
   */
  public String getName() {
    return field.getName();
  }
}
