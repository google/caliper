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
 * @author Kevin Bourrillion
 */
public class Launcher {

  /**
   * Your benchmark classes can implement main() like this: <pre>   {@code
   *
   *   public static void main(String[] args) {
   *     ?.?(MyBenchmark.class, args);
   *   }}</pre>
   *
   * Note that this method does invoke {@link System#exit} when it finishes. Consider {@link
   * com.google.caliper.runner.CaliperMain#exitlessMain} if you don't want that.
   */
  public static void launch(Class<? extends Benchmark> benchmarkClass, String... args) {
    launch(concat(args, benchmarkClass.getName()));
  }

  public static void launch(String... args) {
    // Later we parse the string back into a class again; oh well, it's still cleaner this way

    try {
      // CaliperMain.main(allArgs);
      // Use annoying reflection to skirt backward compilation dependency
      Class<?> mainClass = Class.forName("com.google.caliper.runner.CaliperMain");
      Method mainMethod = mainClass.getMethod("main", String[].class);
      mainMethod.invoke(null, new Object[] {args});
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

  public static String[] concat(String[] array, String element) {
    String[] result = Arrays.copyOf(array, array.length + 1);
    result[array.length] = element;
    return result;
  }
}
