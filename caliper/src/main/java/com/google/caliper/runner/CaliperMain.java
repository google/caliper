/*
 * Copyright (C) 2011 Google Inc.
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

package com.google.caliper.runner;

import static com.google.common.collect.ObjectArrays.concat;

import com.google.caliper.api.Benchmark;
import com.google.caliper.util.HelpRequestedException;
import com.google.caliper.util.InvalidCommandException;
import com.google.common.base.Objects;

import java.io.File;
import java.io.PrintWriter;

/**
 * Primary entry point for the caliper benchmark runner application; run with {@code --help} for
 * details.
 */
public final class CaliperMain {
  /**
   * Form of {@link #main(String[])} more suitable for invoking programmatically; returns the exit
   * code as an {@code int} instead of calling {@link System#exit}.
   */
  public static int main(Class<? extends Benchmark> benchmarkClass, String... args) {
    // Later we parse the string back into a class again; oh well, it's still cleaner this way
    return main2(concat(args, benchmarkClass.getName()));
  }

  /**
   * Primary entry point for the caliper benchmark runner application; run with {@code --help} for
   * details. This method is not intended to be invoked programmatically; see the other overload for
   * that.
   */
  public static void main(String[] args) {
    System.exit(main2(args));
  }

  private static int main2(String[] args) {
    PrintWriter writer = new PrintWriter(System.out);

    File rcFile = new File(Objects.firstNonNull(
          System.getenv("CALIPERRC"),
          System.getProperty("user.home") + "/.caliperrc"));

    try {
      CaliperRun run = Wiring.wireItUp(writer, rcFile, args);
      run.execute();
      return 0;

    } catch (HelpRequestedException e) {
      ParsedOptions.printUsage(writer);
      return 0;

    } catch (InvalidCommandException e) {
      writer.println(e.getMessage());
      writer.println();
      ParsedOptions.printUsage(writer);
      return 1;
    }
  }
}
