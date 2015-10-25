/*
 * Copyright (C) 2013 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */

package com.google.caliper.api;

import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import com.google.common.annotations.Beta;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

/**
 * Apply this annotation to any method without parameters to have it timed as a macrobenchmark. A
 * macrobenchmark is roughly defined as any benchmark whose runtime is large enough that the
 * granularity of the {@linkplain System#nanoTime clock} is not a factor in measurement. Thus, each
 * repetition of the benchmark code can be timed individually.
 *
 * <p>Additionally, since each rep is independently timed, setup and tear down logic can be
 * performed in between each using the {@link BeforeRep} and {@link AfterRep} annotations
 * respectively.
 */
@Retention(RUNTIME)
@Target(METHOD)
@Beta
public @interface Macrobenchmark {}
