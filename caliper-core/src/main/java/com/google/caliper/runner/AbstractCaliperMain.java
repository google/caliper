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


import com.google.caliper.config.InvalidConfigurationException;
import com.google.caliper.options.CaliperOptions;
import com.google.caliper.util.InvalidCommandException;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableSortedMap;
import com.google.common.util.concurrent.Service;
import com.google.common.util.concurrent.ServiceManager;
import java.io.PrintWriter;
import java.util.Map.Entry;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import javax.annotation.Nullable;

/**
 * Abstract implementation of the main process for Caliper; leaves only the creation of the Dagger
 * component that provides dependencies to subclasses.
 */
abstract class AbstractCaliperMain {

  /**
   * Creates the Dagger {@link MainComponent} that will provide the classes needed to run Caliper.
   */
  protected abstract MainComponent createMainComponent(
      String[] args, PrintWriter stdout, PrintWriter stderr);

  /**
   * Entry point for the caliper benchmark runner application; run with {@code --help} for details.
   */
  protected final void mainImpl(String[] args) {
    PrintWriter stdout = new PrintWriter(System.out, true);
    PrintWriter stderr = new PrintWriter(System.err, true);
    int code = mainImpl(args, stdout, stderr);
    System.exit(code);
  }

  protected final int mainImpl(String[] args, PrintWriter stdout, PrintWriter stderr) {
    int code = 1; // pessimism!
    try {
      exitlessMainImpl(args, stdout, stderr);
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
    return code;
  }

  private static final String LEGACY_ENV = "USE_LEGACY_CALIPER";

  protected final void exitlessMainImpl(String[] args, PrintWriter stdout, PrintWriter stderr)
      throws InvalidCommandException, InvalidBenchmarkException, InvalidConfigurationException {
    @Nullable String legacyCaliperEnv = System.getenv(LEGACY_ENV);
    if (!Strings.isNullOrEmpty(legacyCaliperEnv)) {
      System.err.println("Legacy Caliper is no more. " + LEGACY_ENV + " has no effect.");
    }
    try {
      MainComponent mainComponent = createMainComponent(args, stdout, stderr);
      CaliperOptions options = mainComponent.getCaliperOptions();
      if (options.printConfiguration()) {
        stdout.println("Configuration:");
        ImmutableSortedMap<String, String> sortedProperties =
            ImmutableSortedMap.copyOf(mainComponent.getCaliperConfig().properties());
        for (Entry<String, String> entry : sortedProperties.entrySet()) {
          stdout.printf("  %s = %s%n", entry.getKey(), entry.getValue());
        }
      }
      // check that the parameters are valid
      mainComponent.getBenchmarkClass().validateParameters(options.userParameters());
      ServiceManager serviceManager = mainComponent.getServiceManager();
      serviceManager.addListener(new ServiceManager.Listener() {
        @Override
        public void failure(Service service) {
          stderr.println("Service " + service + " failed to start with the following exception:");
          service.failureCause().printStackTrace(stderr);
        }
      });
      serviceManager.startAsync().awaitHealthy();
      try {
        CaliperRun run = mainComponent.getCaliperRun();
        run.run(); // throws IBE
      } finally {
        try {
          // We have some shutdown logic to ensure that files are cleaned up so give it a chance to
          // run
          serviceManager.stopAsync().awaitStopped(10, TimeUnit.SECONDS);
        } catch (TimeoutException e) {
          // That's fine
        }
      }
    } finally {
      // courtesy flush
      stderr.flush();
      stdout.flush();
    }
  }
}
