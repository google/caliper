/*
 * Copyright (C) 2013 Google Inc.
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

package com.google.caliper.util;

import com.google.common.collect.ImmutableSet;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;

/**
 * A utility class for common reflection operations in Caliper.
 */
public final class Reflection {
  private Reflection() {}

  public static ImmutableSet<Method> getAnnotatedMethods(Class<?> clazz,
      Class<? extends Annotation> annotationClass) {
    Method[] methods = clazz.getDeclaredMethods();
    ImmutableSet.Builder<Method> builder = ImmutableSet.builder();
    for (Method method : methods) {
      if (method.isAnnotationPresent(annotationClass)) {
        method.setAccessible(true);
        builder.add(method);
      }
    }
    return builder.build();
  }
}
