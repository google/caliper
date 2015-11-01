/*
 * Copyright (C) 2012 Google Inc.
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

package com.google.caliper.api;

import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

/**
 * Annotates a method whose return value is an object whose total memory footprint is to be
 * measured.
 */
@Retention(RUNTIME)
@Target(METHOD)
public @interface Footprint {
  /**
   * Optionally ignore instances of the specified types (including subclasses) when measuring.  For
   * example, {@code @Footprint(ignore = Element.class) public Set<Element> set() {...}} would
   * measure the size of the set while ignoring the size of the elements.
   */
  Class<?>[] exclude() default {};
}
