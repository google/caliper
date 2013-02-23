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

import com.google.caliper.Benchmark;
import com.google.caliper.bridge.BridgeModule;
import com.google.caliper.config.ConfigModule;
import com.google.caliper.config.InvalidConfigurationException;
import com.google.caliper.json.GsonModule;
import com.google.caliper.options.CaliperOptions;
import com.google.caliper.options.OptionsModule;
import com.google.caliper.util.InvalidCommandException;
import com.google.caliper.util.OutputModule;
import com.google.common.base.Strings;
import com.google.common.base.Throwables;
import com.google.inject.CreationException;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Module;
import com.google.inject.ProvisionException;
import com.google.inject.spi.Message;

import java.io.PrintWriter;

import javax.annotation.Nullable;

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
    PrintWriter stdout = new PrintWriter(System.out, true);
    PrintWriter stderr = new PrintWriter(System.err, true);
    int code = 1; // pessimism!

    try {
      exitlessMain(args, stdout, stderr);
      code = 0;

    } catch (InvalidCommandException e) {
      e.display(stderr);
      code = e.exitCode();

    } catch (InvalidBenchmarkException e) {
      e.display(stderr);

    } catch (InvalidConfigurationException e) {
      e.display(stderr);

    } catch (Throwable t) {
      t.printStackTrace(stderr);
      stdout.println();
      stdout.println("An unexpected exception has been thrown by the caliper runner.");
      stdout.println("Please see https://sites.google.com/site/caliperusers/issues");
    }

    stdout.flush();
    stderr.flush();
    System.exit(code);
  }

  private static final String LEGACY_ENV = "USE_LEGACY_CALIPER";

  public static void exitlessMain(String[] args, PrintWriter stdout, PrintWriter stderr)
      throws InvalidCommandException, InvalidBenchmarkException, InvalidConfigurationException {
    @Nullable String legacyCaliperEnv = System.getenv(LEGACY_ENV);
    if (!Strings.isNullOrEmpty(legacyCaliperEnv)) {
      System.err.println("Legacy Caliper is no more. " + LEGACY_ENV + " has no effect.");
    }
    try {
      // TODO(gak): see if there's a better way to deal with options. probably a module
      Injector optionsInjector = Guice.createInjector(new OptionsModule(args));
      CaliperOptions options = optionsInjector.getInstance(CaliperOptions.class);
      Module runnerModule = new ExperimentingRunnerModule();
      Injector injector = optionsInjector.createChildInjector(
          new OutputModule(stdout, stderr),
          new BridgeModule(),
          new GsonModule(),
          new ConfigModule(),
          runnerModule);
      CaliperRun run = injector.getInstance(CaliperRun.class); // throws wrapped ICE, IBE
      run.run(); // throws IBE
    } catch (CreationException e) {
      propogateIfCaliperException(e.getCause());
      throw e;
    } catch (ProvisionException e) {
      Throwable cause = e.getCause();
      propogateIfCaliperException(e.getCause());
      for (Message message : e.getErrorMessages()) {
        propogateIfCaliperException(message.getCause());
      }
      throw e;
    }

    // courtesy flush
    stderr.flush();
    stdout.flush();
  }

  private static void propogateIfCaliperException(Throwable throwable)
      throws InvalidCommandException, InvalidBenchmarkException, InvalidConfigurationException {
    Throwables.propagateIfInstanceOf(throwable, InvalidCommandException.class);
    Throwables.propagateIfInstanceOf(throwable, InvalidBenchmarkException.class);
    Throwables.propagateIfInstanceOf(throwable, InvalidConfigurationException.class);
  }
}
