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
import com.google.caliper.config.CaliperConfig;
import com.google.caliper.config.CaliperConfigLoader;
import com.google.caliper.util.InvalidCommandException;

import java.io.File;
import java.io.PrintWriter;

/**
 * Primary entry point for the caliper benchmark runner application; run with {@code --help} for
 * details. This class's only purpose is to take care of anything that's specific to command-line
 * invocation and then hand off to {@code CaliperRun}. That is, a hypothetical GUI benchmark runner
 * might still use {@code CaliperRun} but would skip using this class.
 */
public final class CaliperMain {
  /**
   * Your benchmark classes can implement main() like this: <pre>   {@code
   *
   *   public static void main(String[] args) {
   *     CaliperMain.main(MyBenchmark.class, args);
   *   }}</pre>
   *
   * Note that this method does invoke {@link System#exit} when it finishes. Consider {@link
   * #exitlessMain} if you don't want that.
   *
   * <p>Measurement is handled in a subprocess, so it will not use {@code benchmarkClass} itself;
   * the class is provided here only as a shortcut for specifying the full class <i>name</i>. The
   * class that gets loaded later could be completely different.
   */
  public static void main(Class<? extends Benchmark> benchmarkClass, String[] args) {
    main(concat(args, benchmarkClass.getName()));
  }

  /**
   * Entry point for the caliper benchmark runner application; run with {@code --help} for details.
   */
  static void main(String[] args) {
    PrintWriter writer = new PrintWriter(System.out);
    int code = 1; // pessimism!

    try {
      exitlessMain(args, writer);
      code = 0;

    } catch (InvalidCommandException e) {
      e.display(writer);
      code = e.exitCode();

    } catch (InvalidBenchmarkException e) {
      e.display(writer);

    } catch (Throwable t) {
      t.printStackTrace(writer);
      writer.println();
      writer.println("An unexpected exception has been thrown by the caliper runner.");
      writer.println("Please see https://sites.google.com/site/caliperusers/issues");
    }

    writer.flush();
    System.exit(code);
  }

  public static void exitlessMain(String[] args, PrintWriter writer)
      throws InvalidCommandException, InvalidBenchmarkException {

    CaliperOptions options = ParsedOptions.from(args); // throws ICE
    CaliperConfig config = CaliperConfigLoader.loadOrCreate(new File(options.caliperRcFilename()));

    ConsoleWriter console = new DefaultConsoleWriter(writer);

    CaliperRun run = new CaliperRun(options, config, console); // throws ICE, IBE
    run.run(); // throws UCE

    // TODO(kevinb): when exactly do we need to do this?
    writer.flush();
  }
}
