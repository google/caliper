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

package com.google.caliper.util;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.primitives.Primitives.wrap;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;

public class TypedField<C, F> {
  public static TypedField<?, ?> from(Field field) {
    return from(field, field.getDeclaringClass(), field.getType());
  }

  public static <C, F> TypedField<C, F> from(Field field, Class<C> classType, Class<F> fieldType) {
    return new TypedField<C, F>(field, classType, fieldType);
  }

  private final Field field;
  private final Class<C> containingType;
  private final Class<F> fieldType;

  private TypedField(Field field, Class<C> containingType, Class<F> fieldType) {
    this.field = field;
    this.fieldType = wrap(fieldType);
    this.containingType = containingType;

    checkArgument(containingType == field.getDeclaringClass());
    checkArgument(this.fieldType == wrap(field.getType()));
    field.setAccessible(true);
  }

  public F getValueFrom(C object) {
    try {
      return fieldType.cast(field.get(object));
    } catch (IllegalAccessException impossible) {
      throw new AssertionError(impossible);
    }
  }

  public void setValueOn(C object, F value) {
    try {
      field.set(object, value);
    } catch (IllegalAccessException impossible) {
      throw new AssertionError(impossible);
    }
  }

  public String name() {
    return field.getName();
  }

  public Class<F> fieldType() {
    return fieldType;
  }

  public Class<C> containingType() {
    return containingType;
  }

  public <A extends Annotation> A getAnnotation(Class<A> annotationClass) {
    return field.getAnnotation(annotationClass);
  }
}
