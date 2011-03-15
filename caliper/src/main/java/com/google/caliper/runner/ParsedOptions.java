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

import com.google.caliper.spi.Instrument;
import com.google.caliper.util.CommandLineParser;
import com.google.caliper.util.CommandLineParser.Leftovers;
import com.google.caliper.util.CommandLineParser.Option;
import com.google.caliper.util.InvalidCommandException;
import com.google.common.base.Objects;
import com.google.common.base.Splitter;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableSetMultimap;
import com.google.common.collect.Multimap;
import com.google.common.collect.Ordering;

import java.io.File;
import java.io.PrintWriter;
import java.util.Iterator;

public final class ParsedOptions implements CaliperOptions {
  public static ParsedOptions from(String[] args, CaliperRc rc)
      throws InvalidCommandException {
    CommandLineParser<ParsedOptions> parser = CommandLineParser.forClass(ParsedOptions.class);
    ParsedOptions options = new ParsedOptions(rc);
    parser.parseAndInject(args, options);
    return options;
  }

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

  @Override public ImmutableSet<String> benchmarkNames() {
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
  // Warmup -- technically instrument-specific
  // --------------------------------------------------------------------------

  private int warmupSeconds = -1;

  @Option({"-w", "--warmup"})
  private void setWarmupSeconds(int seconds) throws InvalidCommandException {
    dryRunIncompatible("warmup");
    if (seconds < 0 || seconds > 999) {
      throw new InvalidCommandException("warmup must be between 0 and 999 seconds: " + seconds);
    }
    this.warmupSeconds = seconds;
  }

  @Override public int warmupSeconds() {
    return (warmupSeconds >= 0) ? warmupSeconds : rc.defaultWarmupSeconds();
  }

  // --------------------------------------------------------------------------
  // VM specifications
  // --------------------------------------------------------------------------

  private ImmutableList<VirtualMachine> vms = ImmutableList.of(VirtualMachine.hostVm());

  /**
   *
   * @param vmChoice
   * @return
   * @throws InvalidCommandException
   */
  private VirtualMachine findVm(String vmChoice) throws InvalidCommandException {

    String vmSpecFromRc = rc.vmAliases().get(vmChoice);
    if (vmSpecFromRc != null) {
      Iterator<String> parts = Splitter.onPattern("\\s+").split(vmSpecFromRc).iterator();
      String vmExec = parts.next();
      ImmutableList<String> args = ImmutableList.copyOf(parts);
      File vmExecutable = new File(rc.vmBaseDirectory(), vmExec);
      return new VirtualMachine(vmChoice, vmExecutable, args);

    } else {
      File vmExecutable = new File(rc.vmBaseDirectory(), vmChoice);
      if (vmExecutable.isDirectory()) {
        vmExecutable = new File(vmExecutable, "bin/java");
      }
      return new VirtualMachine(vmChoice, vmExecutable, ImmutableList.<String>of());
    }
  }

  @Option({"-m", "--vm"})
  private void setVms(String vmsString) throws InvalidCommandException {
    dryRunIncompatible("vm");

    ImmutableSet<String> vmChoices = split(vmsString);
    ImmutableList.Builder<VirtualMachine> vmsBuilder = ImmutableList.builder();
    for (String vmChoice : vmChoices) {
      VirtualMachine spec = findVm(vmChoice);
      if (!spec.execPath.exists()) {
        throw new InvalidCommandException("VM executable not found: " + spec.execPath);
      }
      vmsBuilder.add(spec);
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

  private Instrument instrument = new MicrobenchmarkInstrument();

  @Option({"-i", "--instrument"})
  private void setInstrument(String instrumentName) throws InvalidCommandException {
    String name = firstNonNull(rc.instrumentAliases().get(instrumentName), instrumentName);
    try {
      instrument = Class.forName(name).asSubclass(Instrument.class).newInstance();
    } catch (Exception e) { // TODO: sloppy, I know
      throw new InvalidCommandException("Invalid instrument: " + instrumentName, e);
    }
  }

  @Override public Instrument instrument() {
    return instrument;
  }

  // --------------------------------------------------------------------------
  // Benchmark parameters
  // --------------------------------------------------------------------------

  private Multimap<String, String> parameterValues = ArrayListMultimap.create();

  @Option("-D")
  private void addParameterSpec(String nameAndValues) throws InvalidCommandException {
    addToMultimap(nameAndValues, parameterValues);
  }

  @Override public ImmutableSetMultimap<String, String> userParameters() {
    // de-dup values, but keep in order
    return new ImmutableSetMultimap.Builder<String, String>()
        .orderKeysBy(Ordering.natural())
        .putAll(parameterValues)
        .build();
  }

  // --------------------------------------------------------------------------
  // VM arguments
  // --------------------------------------------------------------------------

  private Multimap<String, String> vmArguments = ArrayListMultimap.create();

  @Option("-J")
  private void addVmArgumentsSpec(String nameAndValues) throws InvalidCommandException {
    dryRunIncompatible("-J");
    addToMultimap(nameAndValues, vmArguments);
  }

  @Override public ImmutableSetMultimap<String, String> vmArguments() {
    // de-dup values, but keep in order
    return new ImmutableSetMultimap.Builder<String, String>()
        .orderKeysBy(Ordering.natural())
        .putAll(vmArguments)
        .build();
  }

  // --------------------------------------------------------------------------
  // Leftover - benchmark class name
  // --------------------------------------------------------------------------

  private BenchmarkClass benchmarkClass;

  @Leftovers
  private void setLeftovers(ImmutableList<String> leftovers) throws InvalidCommandException {
    if (leftovers.isEmpty()) {
      throw new InvalidCommandException("No benchmark class specified");
    }
    if (leftovers.size() > 1) {
      throw new InvalidCommandException("Extra stuff, expected only class name: " + leftovers);
    }
    try {
      this.benchmarkClass = BenchmarkClass.forName(leftovers.get(0));
    } catch (IllegalArgumentException e) {
      throw new InvalidCommandException(e.getMessage());
    }
  }

  @Override public BenchmarkClass benchmarkClass() {
    return benchmarkClass;
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
        .add("benchmarkClass", this.benchmarkClass())
        .add("benchmarkMethodNames", this.benchmarkNames())
        .add("benchmarkParameters", this.userParameters())
        .add("calculateAggregateScore", this.calculateAggregateScore())
        .add("dryRun", this.dryRun())
        .add("instrumentNames", this.instrument())
        .add("vms", this.vms())
        .add("vmArguments", this.vmArguments())
        .add("outputFileOrDir", this.outputFileOrDir())
        .add("trials", this.trials())
        .add("verbose", this.verbose())
        .add("warmupSeconds", this.warmupSeconds())
        .add("delimiter", this.delimiter)
        .toString();
  }

  // TODO(kevinb): kinda nice if CommandLineParser could autogenerate most of this...
  public static void printUsage(PrintWriter pw) {
    pw.println("Usage:");
    pw.println(" caliper [options...] <benchmark_class_name>");
    pw.println(" java <benchmark_class_name> [options...]");
    pw.println();
    pw.println("Options:");
    pw.println(" -h, --help        print this message");
    pw.println(" -n, --dry-run     instead of measuring, execute a single 'rep' of each");
    pw.println("                   benchmark scenario in-process (for debugging); only options");
    pw.println("                   -b, -d and -D are available in this mode");
    pw.println(" -b, --benchmark   comma-separated list of benchmark methods to run; 'foo' is");
    pw.println("                   an alias for 'timeFoo' (default: all found in class)");
    pw.println(" -m, --vm          comma-separated list of vms to test on; possible values are");
    pw.println("                   configured in ~/.caliperrc (default: only the vm caliper");
    pw.println("                   itself is running in)");
    pw.println(" -i, --instrument  measuring instrument to use; possible values are configured");
    pw.println("                   in ~/.caliperrc (default: 'microbench')");
    pw.println(" -t, --trials      number of independent measurements to take per benchmark");
    pw.println("                   scenario; a positive integer (default: 1)");
    pw.println(" -w, --warmup      minimum time in seconds to warm up before each measurement;");
    pw.println("                   a positive integer (default: 10, or as given in .caliperrc)");
    pw.println(" -o, --output      name of file or directory in which to store the results");
    pw.println("                   data file; if a directory a unique filename is chosen; if a");
    pw.println("                   file it is overwritten (default: ?TODO?)");
    pw.println(" -l, --logging     generate extremely detailed event logs (GC, compilation");
    pw.println("                   events, etc.) and include them in the output data file");
    pw.println("                   (does not affect console display)");
    pw.println(" -v, --verbose     instead of normal console output, display a raw feed of");
    pw.println("                   very detailed information");
    pw.println(" -s, --score       also calculate and display an aggregate score for this run;");
    pw.println("                   higher is better; this score can be compared to other runs");
    pw.println("                   with the exact same arguments but otherwise means nothing");
    pw.println(" -d, --delimiter   separator character used to parse --vm, --benchmark, -D");
    pw.println("                   and -J options (default: ',')");
    pw.println();
    pw.println(" -Dparam=val1,val2,... ");
    pw.println();
    pw.println(" Specifies the values to inject into the 'param' field of the benchmark class;");
    pw.println(" if multiple values or parameters are specified in this way, caliper will test");
    pw.println(" all possible combinations.");
    pw.println();
    pw.println(" -JdisplayName='vm arg list choice 1,vm arg list choice 2,...'");
    pw.println();
    pw.println(" Specifies alternate sets of VM arguments to pass. displayName is any name you");
    pw.println(" would like this variable to appear as in reports. caliper will test all");
    pw.println(" possible combinations. Example: '-Jmemory=-Xms32m -Xmx32m,-Xms512m -Xmx512m'");
    pw.println();
  }
}
