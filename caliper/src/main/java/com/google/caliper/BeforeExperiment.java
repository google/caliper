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

package com.google.caliper;

import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Annotation;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

/**
 * An annotation for methods to be run before an experiment has been performed.
 *
 * @see AfterExperiment
 */
@Target(METHOD)
@Retention(RUNTIME)
public @interface BeforeExperiment {
  /**
   * A qualifier for which types of experiments this method should run. For example, annotating a
   * method with {@code @BeforeExperiment(Benchmark.class)} will cause it to only run for
   * {@link Benchmark} experiments. By default, annotated methods run for all experiments.
   */
  Class<? extends Annotation> value() default All.class;
}
