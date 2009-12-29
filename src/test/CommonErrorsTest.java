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

package test;

import com.google.caliper.Runner;
import junit.framework.TestCase;

/**
 * Unit test covering common user mistakes.
 *
 * @author Kevin Bourrillion
 */
@SuppressWarnings({"HardcodedLineSeparator"}) // TODO: make this pass on Windows
public class CommonErrorsTest extends TestCase {
  public void testDidntSubclassAnything() {
    try {
      Runner.main(BadBenchmark1.class.getName());
    } catch (Exception e) {
      assertEquals("Error: Class [class test.CommonErrorsTest$BadBenchmark1] does not implement the"
          + " interface com.google.caliper.Benchmark interface.\nTypical Remedy: Add 'extends class"
          + " com.google.caliper.SimpleBenchmark' to the class declaration.", e.getMessage());
    }
  }

  static class BadBenchmark1 {
    public void timeSomething(int reps) {
      for (int i = 0; i < reps; i++) {
        System.nanoTime();
      }
    }
  }
}
