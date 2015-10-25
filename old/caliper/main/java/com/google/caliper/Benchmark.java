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

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

/**
 * Annotation for benchmark methods. To write a benchmark:
 *
 * <ol>
 *   <li>Annotate one or more methods with this annotation.
 *   <li>Annotate any fields with {@literal @}{@link Param} that should have parameter values
 *       injected (see {@literal @}{@link Param} for more details)
 *   <li>Optionally use {@link BeforeExperiment} and {@link AfterExperiment} on setup and teardown
 *       methods
 * </ol>
 *
 * <p>Since many benchmarks may execute in a shorter duration than is accurately measured by
 * available timers, benchmark methods <i>may</i> take either an {@code int} or {@code long}
 * argument representing a number of repetitions to perform in a given execution. It is critical
 * that the work done in the benchmark method scale linearly to the number of repetitions.
 *
 * <p>Benchmark methods may return any value. It will be ignored.
 *
 * <p>This class is instantiated and injected only once per child VM invocation, to measure one
 * particular combination of parameters.
 *
 * <p>For example: <pre>   {@code
 *   public final class MyBenchmark {
 *     {@literal @}Param FeatureEnum feature;
 *     {@literal @}Param({"1", "10", "100"}) int size;
 *     private MyObject objectToBenchmark;
 *
 *     {@literal @}BeforeExperiment void initializeObject() {
 *       objectToBenchmark = new MyObject(size);
 *     }
 *
 *     {@literal @}Benchmark int foo(int reps) {
 *       MyObject object = objectToBenchmark;  // copy to local to avoid field access overhead
 *       int dummy = 0;
 *       for (int i = 0; i < reps; i++) {
 *         dummy += object.foo(feature);
 *       }
 *       // return a dummy value so the JIT compiler doesn't optimize away the entire method.
 *       return dummy;
 *     }
 *
 *     {@literal @}Benchmark int bar() {
 *       // benchmark another operation of MyObject that doesn't require a reps parameter
 *     }
 *   }
 * </pre>
 *
 * <p>The benchmark class MyBenchmark has two benchmark methods ({@code foo} and {@code bar}) and
 * two {@link Param Params} ({@code feature} and {@code size}). For each experiment performed by
 * Caliper (e.g. {@code foo} with {@code feature == FeatureEnum.A} and {@code size == 100}),
 * {@code initializeObject} will be called exactly once, but {@code foo} may be called many times.
 */
@Target(METHOD)
@Retention(RUNTIME)
public @interface Benchmark {}
