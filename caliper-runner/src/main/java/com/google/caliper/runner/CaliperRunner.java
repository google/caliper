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

package com.google.caliper.runner;

import com.google.caliper.core.InvalidBenchmarkException;
import com.google.caliper.runner.config.CaliperConfig;
import com.google.caliper.runner.config.InvalidConfigurationException;
import com.google.caliper.runner.options.CaliperOptions;
import com.google.caliper.runner.worker.targetinfo.TargetInfo;
import com.google.caliper.runner.worker.targetinfo.TargetInfoFactory;
import com.google.caliper.util.DisplayUsageException;
import com.google.caliper.util.InvalidCommandException;
import com.google.caliper.util.Stderr;
import com.google.caliper.util.Stdout;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableSortedMap;
import com.google.common.util.concurrent.Service;
import com.google.common.util.concurrent.ServiceManager;
import dagger.Lazy;
import java.io.PrintWriter;
import java.util.Map.Entry;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import javax.inject.Inject;
import javax.inject.Provider;

/**
 * The Caliper runner.
 *
 * <p>This is the entry point class for the Caliper runner process which controls a full benchmark
 * run.
 *
 * @author Colin Decker
 */
public final class CaliperRunner {

  // These core dependencies should be lazy since some of them throw exceptions and we want that
  // to occur inside of run() instead of when constructing this object.
  private final Lazy<CaliperOptions> options;
  private final Lazy<CaliperConfig> config;

  private final Lazy<ServiceManager> serviceManager;

  // TODO(cgdecker): This shouldn't need to be lazy, but it causes problems, weirdly enough, with
  // if the user passes --help on the command line if it isn't lazy. Would be nice to fix that.
  private final Lazy<TargetInfoFactory> targetInfoFactory;

  private final PrintWriter stdout;
  private final PrintWriter stderr;

  private final Provider<CaliperRunComponent.Builder> runComponentBuilder;

  @Inject
  CaliperRunner(
      Lazy<CaliperOptions> options,
      Lazy<CaliperConfig> config,
      Lazy<ServiceManager> serviceManager,
      Lazy<TargetInfoFactory> targetInfoFactory,
      @Stdout PrintWriter stdout,
      @Stderr PrintWriter stderr,
      Provider<CaliperRunComponent.Builder> runComponentBuilder) {
    this.options = options;
    this.config = config;
    this.serviceManager = serviceManager;
    this.targetInfoFactory = targetInfoFactory;
    this.stdout = stdout;
    this.stderr = stderr;
    this.runComponentBuilder = runComponentBuilder;
  }

  /** Runs Caliper, handles any exceptions and returns an exit code. */
  public int run() {
    int code = 1; // pessimism!
    try {
      runInternal();
      code = 0;
    } catch (DisplayUsageException e) {
      e.display(stdout);
      code = e.exitCode();
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

  /**
   * Runs Caliper, handling any exceptions and exiting the process with an appropriate exit code.
   */
  public void runAndExit() {
    System.exit(run());
  }

  /**
   * Runs Caliper, throwing an exception if the command line, configuration or benchmark class is
   * invalid.
   */
  @VisibleForTesting
  public void runInternal()
      throws InvalidCommandException, InvalidBenchmarkException, InvalidConfigurationException {
    try {
      if (options.get().printConfiguration()) {
        stdout.println("Configuration:");
        ImmutableSortedMap<String, String> sortedProperties =
            ImmutableSortedMap.copyOf(config.get().properties());
        for (Entry<String, String> entry : sortedProperties.entrySet()) {
          stdout.printf("  %s = %s%n", entry.getKey(), entry.getValue());
        }
      }
      serviceManager
          .get()
          .addListener(
              new ServiceManager.Listener() {
                @Override
                public void failure(Service service) {
                  stderr.println(
                      "Service " + service + " failed to start with the following exception:");
                  service.failureCause().printStackTrace(stderr);
                }
              });
      serviceManager.get().startAsync().awaitHealthy();
      try {
        TargetInfo targetInfo = targetInfoFactory.get().getTargetInfo();
        CaliperRun run = runComponentBuilder.get().targetInfo(targetInfo).build().getCaliperRun();
        run.run(); // throws IBE
      } finally {
        try {
          // We have some shutdown logic to ensure that files are cleaned up so give it a chance to
          // run
          serviceManager.get().stopAsync().awaitStopped(10, TimeUnit.SECONDS);
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
