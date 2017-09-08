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

import com.google.caliper.core.InvalidBenchmarkException;
import com.google.caliper.runner.config.InvalidConfigurationException;
import com.google.caliper.runner.options.OptionsModule;
import com.google.caliper.runner.platform.JvmPlatform;
import com.google.caliper.runner.platform.PlatformModule;
import com.google.caliper.util.InvalidCommandException;
import com.google.caliper.util.OutputModule;
import com.google.common.annotations.VisibleForTesting;
import java.io.PrintWriter;

/**
 * Primary entry point for the caliper benchmark runner application; run with {@code --help} for
 * details. This class's only purpose is to take care of anything that's specific to command-line
 * invocation and then hand off to {@code CaliperRun}. That is, a hypothetical GUI benchmark runner
 * might still use {@code CaliperRun} but would skip using this class.
 */
public final class CaliperMain {
  /**
   * Your benchmark classes can implement main() like this:
   *
   * <pre>{@code
   * public static void main(String[] args) {
   *   CaliperMain.main(MyBenchmark.class, args);
   * }
   * }</pre>
   *
   * Note that this method does invoke {@link System#exit} when it finishes. Consider {@link
   * #exitlessMain} if you don't want that.
   *
   * <p>Measurement is handled in a subprocess, so it will not use {@code benchmarkClass} itself;
   * the class is provided here only as a shortcut for specifying the full class <i>name</i>. The
   * class that gets loaded later could be completely different.
   */
  public static void main(Class<?> benchmarkClass, String[] args) {
    main(concat(args, benchmarkClass.getName()));
  }

  /**
   * Entry point for the caliper benchmark runner application; run with {@code --help} for details.
   */
  public static void main(String[] args) {
    createRunner(args, new PrintWriter(System.out, true), new PrintWriter(System.err, true))
        .runAndExit();
  }

  public static void exitlessMain(String[] args, PrintWriter stdout, PrintWriter stderr)
      throws InvalidCommandException, InvalidBenchmarkException, InvalidConfigurationException {
    createRunner(args, stdout, stderr).runInternal();
  }

  @VisibleForTesting
  static CaliperRunner createRunner(String[] args, PrintWriter stdout, PrintWriter stderr) {
    return DaggerJvmCaliperRunnerComponent.builder()
        .optionsModule(OptionsModule.withBenchmarkClass(args))
        .outputModule(new OutputModule(stdout, stderr))
        .platformModule(new PlatformModule(new JvmPlatform()))
        .build()
        .getRunner();
  }
}
