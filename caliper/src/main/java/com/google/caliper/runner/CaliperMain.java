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
import com.google.common.base.Objects;

import java.io.File;
import java.io.PrintWriter;

/**
 * Primary entry point for the caliper benchmark runner application; run with {@code --help} for
 * details.
 */
public final class CaliperMain {
  /**
   * Primary entry point for the caliper benchmark runner application; run with {@code --help} for
   * details. This method is not intended to be invoked programmatically.
   */
  public static void main(String[] args) {
    PrintWriter writer = new PrintWriter(System.out);
    int code = 0;
    try {
      exitlessMain(args);

    } catch (HelpRequestedException e) {
      ParsedOptions.printUsage(writer);

    } catch (InvalidCommandException e) {
      writer.println(e.getMessage());
      writer.println();
      ParsedOptions.printUsage(writer);
      code = 1;

    } catch (UserCodeException e) {
      // This is the user's exception, not ours, so print a stack trace
      // TODO(kevinb): trim caliper frames
      e.printStackTrace(writer);
      code = 1;

    } catch (InvalidBenchmarkException e) {
      writer.println(e.getMessage());
      code = 1;
    }

    writer.flush();
    System.exit(code);
  }

  public static void exitlessMain(String[] args)
      throws InvalidCommandException, InvalidBenchmarkException {
    PrintWriter writer = new PrintWriter(System.out);

    String rcFilename = Objects.firstNonNull(
          System.getenv("CALIPERRC"),
          System.getProperty("user.home") + "/.caliperrc");

    CaliperRc rc = CaliperRcManager.loadOrCreate(new File(rcFilename));

    CaliperOptions options = ParsedOptions.from(args, rc); // throws ICE
    ConsoleWriter console = new DefaultConsoleWriter(writer);

    CaliperRun run = new CaliperRun(options, rc, console); // throws ICE, IBE
    run.run(); // throws UCE
  }
}
