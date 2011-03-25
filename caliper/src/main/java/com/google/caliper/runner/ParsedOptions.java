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

import static com.google.common.base.Objects.firstNonNull;
import static com.google.common.base.Preconditions.checkNotNull;

import com.google.caliper.util.CommandLineParser;
import com.google.caliper.util.CommandLineParser.Leftovers;
import com.google.caliper.util.CommandLineParser.Option;
import com.google.caliper.util.InvalidCommandException;
import com.google.caliper.util.Util;
import com.google.common.base.Objects;
import com.google.common.base.Splitter;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableSetMultimap;
import com.google.common.collect.Multimap;
import com.google.common.collect.Ordering;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

public final class ParsedOptions implements CaliperOptions {
  public static ParsedOptions from(String[] args, CaliperRc rc)
      throws InvalidCommandException {
    ParsedOptions options = new ParsedOptions(rc);

    CommandLineParser<ParsedOptions> parser = CommandLineParser.forClass(ParsedOptions.class);
    try {
      parser.parseAndInject(args, options);
    } catch (InvalidCommandException e) {
      e.setUsage(USAGE);
      throw e;
    }
    return options;
  }

  // TODO(kevinb): consider leaving this out of it; look up VMs and Instruments in the next step
  private final CaliperRc rc;

  private ParsedOptions(CaliperRc rc) {
    this.rc = checkNotNull(rc);
  }

  // --------------------------------------------------------------------------
  // Dry run -- simple boolean, needs to be checked in some methods
  // --------------------------------------------------------------------------

  // TODO(kevinb): remove legacy --debug alias
  @Option({"-n", "--dry-run", "--debug"})
  private boolean dryRun;

  @Override public boolean dryRun() {
    return dryRun;
  }

  private void dryRunIncompatible(String optionName) throws InvalidCommandException {
    // This only works because CLP does field injection before method injection
    if (dryRun) {
      throw new InvalidCommandException("Option not available in dry-run mode: " + optionName);
    }
  }

  // --------------------------------------------------------------------------
  // Delimiter -- injected early so methods can use it
  // --------------------------------------------------------------------------

  @Option({"-d", "--delimiter"})
  private String delimiter = ",";

  private ImmutableSet<String> split(String string) {
    return ImmutableSet.copyOf(Splitter.on(delimiter).split(string));
  }

  // --------------------------------------------------------------------------
  // Benchmark method names to run
  // --------------------------------------------------------------------------

  private ImmutableSet<String> benchmarkNames = ImmutableSet.of();

  @Option({"-b", "--benchmark"})
  private void setBenchmarkNames(String benchmarksString) {
    benchmarkNames = split(benchmarksString);
  }

  @Override public ImmutableSet<String> benchmarkMethodNames() {
    return benchmarkNames;
  }

  // --------------------------------------------------------------------------
  // Verbose?
  // --------------------------------------------------------------------------

  @Option({"-v", "--verbose"})
  private boolean verbose = false;

  @Override public boolean verbose() {
    return verbose;
  }

  // --------------------------------------------------------------------------
  // Generate detailed logs?
  // --------------------------------------------------------------------------

  private boolean detailedLogging = false;

  // TODO(kevinb): remove legacy --captureVmLog alias
  @Option({"-l", "--logging", "--captureVmLog"})
  private void setDetailedLogging(boolean b) throws InvalidCommandException {
    if (b) {
      dryRunIncompatible("verbose");
    }
    this.detailedLogging = b;
  }

  @Override public boolean detailedLogging() {
    return detailedLogging;
  }

  // --------------------------------------------------------------------------
  // Trials
  // --------------------------------------------------------------------------

  private int trials = 1;

  @Option({"-t", "--trials"})
  private void setTrials(int trials) throws InvalidCommandException {
    dryRunIncompatible("trials");
    if (trials < 1) {
      throw new InvalidCommandException("trials must be at least 1: " + trials);
    }
    this.trials = trials;
  }

  @Override public int trials() {
    return trials;
  }

  // --------------------------------------------------------------------------
  // VM specifications
  // --------------------------------------------------------------------------

  private ImmutableList<VirtualMachine> vms = ImmutableList.of(VirtualMachine.hostVm());

  private VirtualMachine findVm(String vmName) {
    String home = firstNonNull(rc.homeDirForVm(vmName), vmName);
    String absoluteHome = home.startsWith("/") ? home : rc.vmBaseDirectory() + "/" + home;
    return VirtualMachine.from(vmName, absoluteHome, rc.vmArgsForVm(vmName));
  }

  @Option({"-m", "--vm"})
  private void setVms(String vmsString) throws InvalidCommandException {
    dryRunIncompatible("vm");

    // TODO(kevinb): review all set/list
    ImmutableSet<String> vmChoices = split(vmsString);

    ImmutableList.Builder<VirtualMachine> vmsBuilder = ImmutableList.builder();
    for (String vmChoice : vmChoices) {
      vmsBuilder.add(findVm(vmChoice));
    }
    this.vms = vmsBuilder.build();
  }

  @Override public ImmutableList<VirtualMachine> vms() {
    return vms;
  }

  // --------------------------------------------------------------------------
  // Output file or dir
  // --------------------------------------------------------------------------

  private String outputFileOrDir;

  // TODO(kevinb): remove legacy --saveResults alias
  @Option({"-o", "--output", "--saveResults"})
  private void setOutputFileOrDir(String s) throws InvalidCommandException {
    dryRunIncompatible("output");
    this.outputFileOrDir = s;
  }

  @Override public String outputFileOrDir() {
    return outputFileOrDir;
  }

  // --------------------------------------------------------------------------
  // Calculate aggregate score?
  // --------------------------------------------------------------------------

  private boolean calculateAggregateScore;

  // Undocumented feature?
  // TODO(kevinb): remove legacy --printScore alias
  @Option({"-s", "--score", "--printScore"})
  private void setCalculateAggregateScore(boolean b) throws InvalidCommandException {
    if (b) {
      dryRunIncompatible("score");
    }
    this.calculateAggregateScore = b;
  }

  @Override public boolean calculateAggregateScore() {
    return calculateAggregateScore;
  }

  // --------------------------------------------------------------------------
  // Measuring instruments to use
  // --------------------------------------------------------------------------

  @Option({"-i", "--instrument"})
  private String instrumentName = "micro";

  @Override public String instrumentName() {
    return instrumentName;
  }

// --------------------------------------------------------------------------
  // Benchmark parameters
  // --------------------------------------------------------------------------

  private Multimap<String, String> mutableUserParameters = ArrayListMultimap.create();

  @Option("-D")
  private void addParameterSpec(String nameAndValues) throws InvalidCommandException {
    addToMultimap(nameAndValues, mutableUserParameters);
  }

  @Override public ImmutableSetMultimap<String, String> userParameters() {
    // de-dup values, but keep in order
    return new ImmutableSetMultimap.Builder<String, String>()
        .orderKeysBy(Ordering.natural())
        .putAll(mutableUserParameters)
        .build();
  }

  // --------------------------------------------------------------------------
  // VM arguments
  // --------------------------------------------------------------------------

  private Multimap<String, String> mutableVmArguments = ArrayListMultimap.create();

  @Option("-J")
  private void addVmArgumentsSpec(String nameAndValues) throws InvalidCommandException {
    dryRunIncompatible("-J");
    addToMultimap(nameAndValues, mutableVmArguments);
  }

  @Override public ImmutableSetMultimap<String, String> vmArguments() {
    // de-dup values, but keep in order
    return new ImmutableSetMultimap.Builder<String, String>()
        .orderKeysBy(Ordering.natural())
        .putAll(mutableVmArguments)
        .build();
  }

  // --------------------------------------------------------------------------
  // Leftover - benchmark class name
  // --------------------------------------------------------------------------

  private String benchmarkClassName;

  @Leftovers
  private void setLeftovers(ImmutableList<String> leftovers) throws InvalidCommandException {
    if (leftovers.isEmpty()) {
      throw new InvalidCommandException("No benchmark class specified");
    }
    if (leftovers.size() > 1) {
      throw new InvalidCommandException("Extra stuff, expected only class name: " + leftovers);
    }
    this.benchmarkClassName = leftovers.get(0);
  }

  @Override public String benchmarkClassName() {
    return benchmarkClassName;
  }

  // --------------------------------------------------------------------------
  // Helper methods
  // --------------------------------------------------------------------------

  private void addToMultimap(String nameAndValues, Multimap<String, String> multimap)
      throws InvalidCommandException {
    int eq = nameAndValues.indexOf('=');
    if (eq == -1) {
      throw new InvalidCommandException("no '=' found in: " + nameAndValues);
    }
    String name = nameAndValues.substring(0, eq);
    String values = nameAndValues.substring(eq + 1);

    if (multimap.containsKey(name)) {
      throw new InvalidCommandException("multiple parameter sets for: " + name);
    }
    multimap.putAll(name, split(values));
  }

  @Override public String toString() {
    return Objects.toStringHelper(this)
        .add("benchmarkClassName", this.benchmarkClassName())
        .add("benchmarkMethodNames", this.benchmarkMethodNames())
        .add("benchmarkParameters", this.userParameters())
        .add("calculateAggregateScore", this.calculateAggregateScore())
        .add("dryRun", this.dryRun())
        .add("instrumentName", this.instrumentName())
        .add("vms", this.vms())
        .add("vmArguments", this.vmArguments())
        .add("outputFileOrDir", this.outputFileOrDir())
        .add("trials", this.trials())
        .add("detailedLogging", this.detailedLogging())
        .add("verbose", this.verbose())
        .add("delimiter", this.delimiter)
        .toString();
  }

  // --------------------------------------------------------------------------
  // Usage
  // --------------------------------------------------------------------------

  // TODO(kevinb): kinda nice if CommandLineParser could autogenerate most of this...
  // TODO(kevinb): a test could actually check that we don't exceed 79 columns.
  private static final ImmutableList<String> USAGE = ImmutableList.of(
      "Usage:",
      " caliper [options...] <benchmark_class_name>",
      " java <benchmark_class_name> [options...]",
      "",
      "Options:",
      " -h, --help        print this message",
      " -n, --dry-run     instead of measuring, execute a single rep for each",
      "                   scenario in-process",
      " -b, --benchmark   comma-separated list of benchmark methods to run; 'foo' is",
      "                   an alias for 'timeFoo' (default: all found in class)",
      " -m, --vm          comma-separated list of vms to test on; possible values are",
      "                   configured in ~/.caliperrc (default: only the vm caliper",
      "                   itself is running in)",
      " -i, --instrument  measuring instrument to use; possible values are configured",
      "                   in ~/.caliperrc (default: 'micro')",
      " -t, --trials      number of independent measurements to take per benchmark",
      "                   scenario; a positive integer (default: 1)",
      " -o, --output      name of file or directory in which to store the results",
      "                   data file; if a directory a unique filename is chosen; if a",
      "                   file it is overwritten (default: ?TODO?)",
      " -l, --logging     generate extremely detailed event logs (GC, compilation",
      "                   events, etc.) and include them in the output data file",
      " -v, --verbose     instead of normal console output, display a raw feed of",
      "                   very detailed information",
      " -s, --score       also calculate and display an aggregate score for this run",
      "                   (higher is better; meaningless otherwise)",
      " -d, --delimiter   separator used in -m, -b, -D and -J options (default: ',')",
      "",
      " -Dparam=val1,val2,... ",
      "     Specifies the values to inject into the 'param' field of the benchmark",
      "     class; if multiple values or parameters are specified in this way,",
      "     caliper will try all possible combinations.",
      " -JdisplayName='vm arg list choice 1,vm arg list choice 2,...'",
      "     Specifies alternate sets of VM arguments to pass. As with any variable,",
      "     caliper will test all possible combinations. Example: ",
      "     -Jmemory='-Xms32m -Xmx32m,-Xms512m -Xmx512m'",
      "",
      "See http://sites.google.com/site/caliperusers/command-line for more details.",
      "");
}
