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

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

/**
 * @author Kevin Bourrillion
 */
public final class ReflectionHelper {
  private ReflectionHelper() {}

  static Object invokeStatic(Method staticMethod) {
    if (!Modifier.isStatic(staticMethod.getModifiers())) {
      throw new InvalidBenchmarkException("Method expected to be static " + staticMethod);
    }
    staticMethod.setAccessible(true);
    try {
      return staticMethod.invoke(null);
    } catch (IllegalAccessException e) {
      throw new AssertionError(e);
    } catch (InvocationTargetException e) {
      throw new UserCodeException(e.getCause());
    }
  }

  static Object getStatic(Field staticField) {
    staticField.setAccessible(true);
    if (!Modifier.isStatic(staticField.getModifiers())) {
      throw new InvalidBenchmarkException("Field expected to be static " + staticField);
    }
    try {
      return staticField.get(null);
    } catch (IllegalAccessException e) {
      throw new AssertionError(e);
    }
  }
}
