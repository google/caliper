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

package com.google.caliper.api;

import java.lang.reflect.Method;
import java.util.Arrays;

/**
 * This is the class users must extend.
 * TODO(kevinb): full javadoc.
 */
public abstract class Benchmark {
  protected Benchmark() {}

  public void setUp() throws Exception {}

  public void tearDown() throws Exception {}

  /**
   * Your benchmark classes can implement main() like this: <pre>   {@code
   *
   *   public static void main(String[] args) {
   *     Benchmark.main(MyBenchmark.class, args);
   *   }}</pre>
   *
   * Note that this method does invoke {@link System#exit} when it finishes. Consider {@link
   * com.google.caliper.runner.CaliperMain#exitlessMain} if you don't want that.
   */
  protected static void main(Class<? extends Benchmark> selfClass, String... args) {
    // Later we parse the string back into a class again; oh well, it's still cleaner this way
    String[] allArgs = concat(args, selfClass.getName());

    try {
      // CaliperMain.main(allArgs);
      // Use annoying reflection to skirt backward compilation dependency
      Class<?> mainClass = Class.forName("com.google.caliper.runner.CaliperMain");
      Method mainMethod = mainClass.getMethod("main", String[].class);
      mainMethod.invoke(null, new Object[] {allArgs});
      System.exit(0);
      
    } catch (ClassNotFoundException e) {
      System.err.println("ERROR: Caliper is not found in your CLASSPATH.");
      System.exit(1);

    } catch (Exception e) {
      // Do this better some time?
      e.printStackTrace();
      System.exit(1);
    }
  }

  private static String[] concat(String[] array, String element) {
    String[] result = Arrays.copyOf(array, array.length + 1);
    result[array.length] = element;
    return result;
  }
}
