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

import com.google.caliper.util.HelpRequestedException;
import com.google.caliper.util.InvalidCommandException;

import java.io.PrintWriter;

public final class CaliperMain {
  private CaliperMain() {}

  /**
   * Convenient form of {@link #main(String[])} that appends the name of {@code benchmarkClass} to
   * the argument list.
   */
  public static void main(Class<? /*extends Benchmark*/> benchmarkClass, String... args) {
    PrintWriter writer = new PrintWriter(System.out);
    CaliperOptions options = null;

    try {
      options = ParsedOptions.from(benchmarkClass, args);

    } catch (HelpRequestedException e) {
      ParsedOptions.printUsage(writer);
      System.exit(0);

    } catch (InvalidCommandException e) {
      writer.println(e.getMessage());
      writer.println();
      ParsedOptions.printUsage(writer);
      System.exit(1); // technically should exit(0) for --help...
    }

    new CaliperRun(options, writer).run();
  }

  /**
   * Main entry point for the caliper benchmark runner application; run with --help for details.
   * This method is not intented to be invoked programmatically; see the other overload for that.
   */
  public static void main(String[] args) {
    // I dislike the null trick but it saves a ton of repetition...
    main(null, args);
  }
}
