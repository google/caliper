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

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * To make your benchmark depend on a parameterized value, create a field with the name you want
 * this parameter to be known by, and add this annotation. Caliper will inject a value for this
 * field to each instance it creates. These values come from
 *
 * <ul>
 * <li>The command line, if specified using {@code -Dname=value1,value2,value3}
 * <li>Otherwise, the {@link #value()} list given in the annotation
 * <li>Otherwise, if the parameter type is either {@code boolean} or an {@code enum} type, Caliper
 *     assumes you want all possible values.
 * <li>Finally, if none of the above match, Caliper will display an error and exit.
 * </ul>
 *
 * <p>Caliper parameters are always strings, but can be converted to other types at the point of
 * injection. If the type of the field this annotation is applied to is not {@link String}, then the
 * type class must contain a static {@code fromString(String)}, {@code decode(String)} or {@code
 * valueOf(String)} method that returns that type, or a constructor accepting only a {@code String}.
 *
 * <p>Caliper will test every possible combination of parameter values for your benchmark. For
 * example, if you have two parameters, {@code -Dletter=a,b,c -Dnumber=1,2}, Caliper will construct
 * six independent "scenarios" and perform measurement for each one.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface Param {
  /**
   * One or more default values, as strings, that this parameter should be given if none are
   * specified on the command line. If values are specified on the command line, the defaults given
   * here are all ignored.
   */
  String[] value() default {};
}
