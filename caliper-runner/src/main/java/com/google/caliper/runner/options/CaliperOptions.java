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

package com.google.caliper.runner.options;

import com.google.caliper.util.ShortDuration;
import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableSetMultimap;
import java.io.File;

/** Options provided by the user on the command line when starting the Caliper runner. */
public interface CaliperOptions {

  // TODO(cgdecker): Does it really make sense to have an interface here?
  // There's only one implementation (ParsedOptions).

  /** Returns the array of command line args that were passed to the Caliper runner as a list. */
  ImmutableList<String> allArgs();

  /** Returns the fully-qualified name of the benchmark class. */
  String benchmarkClassName();

  /**
   * Returns the names of the benchmark methods to be benchmarked. If none are specified, all
   * benchmark methods on the benchmark class are used.
   */
  ImmutableSet<String> benchmarkMethodNames();

  /** Returns the name of the configured device that the benchmark should be run on. */
  String deviceName();

  /**
   * Returns the set of VMs the benchmark should be run on. If none are specified, the default VM
   * for the device is used.
   */
  ImmutableSet<String> vmNames();

  /**
   * Returns values to use for the benchmark class's {@code @Param} fields. For any parameter field
   * not included here, the default values for the field are used.
   */
  ImmutableSetMultimap<String, String> userParameters();

  /**
   * Returns sets of VM arguments to test with. Keys are names for the argument sets and values are
   * the arguments to include in that set.
   *
   * <p><b>NOTE:</b> This option is currently unused. The intent was apparently that this be another
   * option that Caliper factors in when determining the set of scenarios to test (i.e. if you
   * specify multiple sets of VM options, each scenario is run separately with each of those arg
   * sets so you can compare results between them).
   */
  // TODO(cgdecker): Either remove this or make it work
  ImmutableSetMultimap<String, String> vmArguments();

  /** Returns the worker classpath to use for the given VM type. */
  Optional<String> workerClasspath(String vmType);

  /**
   * Returns additional Caliper configuration properties to be merged with the properties in the
   * global and user Caliper configuration files.
   */
  ImmutableMap<String, String> configProperties();

  /** Returns the names of the instruments to be used for the benchmark. */
  ImmutableSet<String> instrumentNames();

  /** Returns the number of trials that should be run per benchmark scenario. */
  int trialsPerScenario();

  /** Returns the time limit to use for each trial. */
  ShortDuration timeLimit();

  /** Returns the name to give this benchmark run. */
  String runName();

  /** Returns whether or not to print the configuration to the console. */
  boolean printConfiguration();

  /** Returns whether or not to only do a dry run of the benchmark. */
  boolean dryRun();

  /** Returns the local port that should be used for reverse proxy. */
  int localPort();

  /** Returns the directory where Caliper configuration, logs, etc. are found. */
  File caliperDirectory();

  /** Returns the user's Caliper configuration file. */
  File caliperConfigFile();

  /** Returns additional arguments to pass to ADB commands. */
  ImmutableList<String> adbArgs();

  /** Returns whether to keep the Android app installed after a run. */
  boolean keepAndroidApp();

  /**
   * Returns whether or not the worker log file content should be printed when a worker fails. By
   * default (false), just the path to the log file is printed.
   */
  boolean printWorkerLog();
}
