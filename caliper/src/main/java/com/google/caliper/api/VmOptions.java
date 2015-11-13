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

package com.google.caliper.api;

import com.google.common.annotations.Beta;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * An annotation applied to a benchmark that specifies flags to be applied to the VM. These flags
 * are applied before those specified on the command-line and thus are not guaranteed to be applied.
 *
 * <p>This API is likely to change.
 */
// TODO(gak): Support platform (e.g. Android and Jvm) specific options.
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@Beta
public @interface VmOptions {
  String[] value() default {};
}
