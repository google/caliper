/*
 * Copyright (C) 2018 Google Inc.
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

package com.google.caliper.runner.config;

import java.util.Arrays;

/** Enumeration of supported VM types in Caliper. */
public enum VmType {
  /** Java Virtual Machine. */
  JVM,
  /** Android virtual machine. */
  ANDROID;

  /** Gets the {@link VmType} for the given {@code type} field string. */
  public static VmType of(String type) {
    switch (type) {
      case "jvm":
        return JVM;
      case "android":
        return ANDROID;
      default:
        throw new InvalidConfigurationException("Invalid VM type: " + type);
    }
  }

  final String name;

  VmType() {
    this.name = name().toLowerCase();
  }

  /**
   * Returns true if the given class is annotated with {@link SupportsVmType} with this {@link
   * VmType} included in its values; false otherwise.
   */
  public final boolean supports(Class<?> clazz) {
    SupportsVmType annotation = clazz.getAnnotation(SupportsVmType.class);
    return annotation != null && Arrays.asList(annotation.value()).contains(this);
  }

  @Override
  public String toString() {
    return name;
  }
}
