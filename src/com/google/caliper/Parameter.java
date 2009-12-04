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

import java.lang.reflect.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Adapts a parameter for a {@link DefaultBenchmarkSuite}.
 */
abstract class Parameter<T> {

  private final Field field;

  private Parameter(Field field) {
    this.field = field;
  }

  /**
   * Returns all properties for the given class.
   */
  public static List<Parameter<?>> forClass(Class<? extends BenchmarkSuite> suiteClass) {
    List<Parameter<?>> parameters = new ArrayList<Parameter<?>>();
    for (final Field field : suiteClass.getDeclaredFields()) {
      if (field.isAnnotationPresent(Param.class)) {
        field.setAccessible(true);
        parameters.add(Parameter.forField(suiteClass, field));
      }
    }
    return parameters;
  }

  public static Parameter forField(
      Class<? extends BenchmarkSuite> suiteClass, final Field field) {
    Parameter result = null;
    Type returnType = null;
    Member member = null;

    try {
      final Method valuesMethod = suiteClass.getDeclaredMethod(field.getName() + "Values");
      valuesMethod.setAccessible(true);
      member = valuesMethod;
      returnType = valuesMethod.getGenericReturnType();
      result = new Parameter<Object>(field) {
        @SuppressWarnings("unchecked") // guarded below
        public Iterable<Object> values() throws Exception {
          return (Iterable<Object>) valuesMethod.invoke(null);
        }
      };
    } catch (NoSuchMethodException ignored) {
    }

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
        public Iterable<Object> values() throws Exception {
          return (Iterable<Object>) valuesField.get(null);
        }
      };
    } catch (NoSuchFieldException ignored) {
    }

    if (result == null) {
      throw new ConfigurationException("No values member defined for " + field);
    }

    if (!Modifier.isStatic(member.getModifiers())) {
      throw new ConfigurationException("Values member must be static " + member);
    }

    // validate return type
    boolean valid = false;
    if (returnType instanceof ParameterizedType) {
      ParameterizedType type = (ParameterizedType) returnType;
      if (type.getRawType() == Iterable.class) {
        valid = true;
      }
    }

    if (!valid) {
      throw new ConfigurationException("Invalid return type " + returnType
          + " for values member " + member + "; must be Iterable");
    }

    return result;
  }

  /**
   * Sets the value of this property to the specified value for the given suite.
   */
  public void set(BenchmarkSuite suite, Object value) throws Exception {
    field.set(suite, value);
  }

  /**
   * Returns the available values of the property as specified by the suite.
   */
  public abstract Iterable<T> values() throws Exception;

  /**
   * Returns the field's name.
   */
  public String getName() {
    return field.getName();
  }
}