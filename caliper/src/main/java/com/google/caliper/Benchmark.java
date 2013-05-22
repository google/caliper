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

package com.google.caliper;

/**
 * Superclass for user benchmark classes to extend. To write a benchmark:
 *
 * <ol>
 *   <li>Extend this class
 *   <li>Annotate any fields with {@literal @}{@link Param} that should have parameter values
 *      injected (see {@literal @}{@link Param} for more details)
 *   <li>Optionally override {@link #setUp setUp} and {@link #tearDown tearDown}
 *   <li>Add one or more benchmark methods (for example, the standard microbenchmark instrument
 *       recognizes a method beginning with the word "time" and taking a single int or long
 *       parameter as a benchmark method)
 * </ol>
 *
 * This class is instantiated and injected only once per child VM invocation, to measure one
 * particular combination of parameters.
 *
 * For example: <pre>   {@code
 *   public class MyBenchmark extends Benchmark {
 *     {@literal @}Param FeatureEnum feature;
 *     {@literal @}Param({"1", "10", "100"}) int size;
 *     private MyObject objectToBenchmark;
 * 
 *     {@literal @}Override protected void setUp() {
 *       objectToBenchmark = new MyObject(size);
 *     }
 *
 *     public int timeFoo(int reps) {
 *       MyObject object = objectToBenchmark;  // copy to local to avoid field access overhead
 *       int dummy = 0;
 *       for (int i = 0; i < reps; i++) {
 *         dummy += object.foo(feature);
 *       }
 *       // return a dummy value so the JIT compiler doesn't optimize away the entire method.
 *       return dummy;
 *     }
 *
 *     public int timeBar(int reps) {
 *       // benchmark another operation of MyObject
 *     }
 *   }
 * </pre>
 *
 * The benchmark class MyBenchmark has two benchmark methods ({@code timeFoo} and {@code timeBar})
 * and two {@link Param Params}. For each experiment performed by caliper (e.g.
 * {@code timeFoo} with {@code feature==FeatureEnum.A} and {@code size == 100}), {@link #setUp} will
 * be called exactly once, but {@code timeFoo} may be called many times.
 */
public abstract class Benchmark {
  protected Benchmark() {}

  /**
   * Sets up the fixture. For example, generate some data structures.  The time it takes to run this
   * method will not be included in the benchmark time.
   *
   * <p>Benchmarks can throw {@link com.google.caliper.api.SkipThisScenarioException
   * SkipThisScenarioException} if the particular combination of parameters injected into the
   * benchmark does not need to be measured.
   */
  protected void setUp() throws Exception {}

  /**
   * Tears down the fixture. The time it takes to run this method will not be included in the
   * benchmark time.
   */
  protected void tearDown() throws Exception {}
}
