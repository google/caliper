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

import static com.google.common.base.Preconditions.checkNotNull;

import com.google.caliper.util.CommandLineParser;
import com.google.caliper.util.CommandLineParser.Option;
import com.google.caliper.util.InvalidCommandException;
import com.google.common.base.Objects;
import com.google.common.base.Splitter;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.Multimap;

import java.io.File;
import java.io.PrintWriter;

public final class ParsedOptions implements CaliperOptions {
  public static CaliperOptions from(
      Class<? /*extends Benchmark*/> benchmarkClass, String... rawArguments)
      throws InvalidCommandException {
    ParsedOptions options = new ParsedOptions();
    if (benchmarkClass != null) {
      options.parse(benchmarkClass, rawArguments);
    } else {
      options.parse(rawArguments);
    }
    return options;
  }

  private final CommandLineParser<ParsedOptions> parser = CommandLineParser.forClass(getClass());

  private ParsedOptions() {}

  void parse(Class<? /*extends Benchmark*/> benchmarkClass, String[] rawArguments)
      throws InvalidCommandException {
    this.benchmarkClass = checkNotNull(benchmarkClass);
    ImmutableList<String> leftovers = parser.parse(rawArguments, this);
    if (!leftovers.isEmpty()) {
      throw new InvalidCommandException("Extra stuff on command line: " + leftovers);
    }
  }

  void parse(String[] rawArguments) throws InvalidCommandException {
    ImmutableList<String> leftovers = parser.parse(rawArguments, this);

    if (leftovers.isEmpty()) {
      throw new InvalidCommandException("No benchmark class specified");
    }
    if (leftovers.size() > 1) {
      throw new InvalidCommandException("Extra stuff, expected only class name: " + leftovers);
    }
    try {
      benchmarkClass = Class.forName(leftovers.get(0));
    } catch (ClassNotFoundException e) {
      throw new InvalidCommandException("Not a benchmark class: " + benchmarkClass);
    }
  }

  // --------------------------------------------------------------------------
  // Dry run -- simple boolean, needs to be checked in some methods
  // --------------------------------------------------------------------------

  @Option({"-n", "--dry-run"})
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

  private ImmutableList<String> split(String string) {
    return ImmutableList.copyOf(Splitter.on(delimiter).split(string));
  }

  // --------------------------------------------------------------------------
  // Benchmark method names to run
  // --------------------------------------------------------------------------

  private ImmutableList<String> benchmarkMethodNames = ImmutableList.of();

  @Option({"-m", "--method"})
  private void setBenchmarkMethodNames(String benchmarksString) {
    benchmarkMethodNames = split(benchmarksString);
  }

  @Override public ImmutableList<String> benchmarkMethodNames() {
    return benchmarkMethodNames;
  }

  // --------------------------------------------------------------------------
  // Verbose?
  // --------------------------------------------------------------------------

  private boolean verbose;

  @Option({"-v", "--verbose"})
  private void setVerbose(boolean b) throws InvalidCommandException {
    if (b) {
      dryRunIncompatible("verbose");
    }
    this.verbose = b;
  }

  @Override public boolean verbose() {
    return verbose;
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
  // Warmup
  // --------------------------------------------------------------------------

  private int warmupSeconds = 10;

  @Option({"-w", "--warmup"})
  private void setWarmupSeconds(int seconds) throws InvalidCommandException {
    dryRunIncompatible("warmup");
    if (seconds < 0 || seconds > 999) {
      throw new InvalidCommandException("warmup must be between 0 and 999 seconds: " + seconds);
    }
    this.warmupSeconds = seconds;
  }

  @Override public int warmupSeconds() {
    return warmupSeconds;
  }

  // --------------------------------------------------------------------------
  // JRE home directories
  // --------------------------------------------------------------------------

  // Let this be injected early (field injection happens first)
  @Option({"-b", "--jre-base"})
  private String jreBaseDir;

  @Override public String jreBaseDir() {
    return jreBaseDir;
  }

  private ImmutableList<String> jreHomes;

  @Option({"-j", "--jre-home"})
  private void setJreHomes(String jreHomesString) throws InvalidCommandException {
    dryRunIncompatible("jre-home");
    jreHomes = split(jreHomesString);

    for (String jre : jreHomes) {
      File home = new File(jre);
      if (!home.isAbsolute() && jreBaseDir != null) {
        home = new File(jreBaseDir, jre);
      }
      File bin = new File(home, "bin");
      if (!new File(bin, "java").exists()) {
        throw new InvalidCommandException("Not a JRE home: " + home);
      }
    }
  }

  @Override public ImmutableList<String> jreHomeDirs() {
    return jreHomes;
  }

  // --------------------------------------------------------------------------
  // Output file or dir
  // --------------------------------------------------------------------------

  private String outputFileOrDir;

  @Option({"-o", "--output"})
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

  @Option({"-s", "--score"})
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

  private ImmutableList<String> instrumentNames = ImmutableList.of("time");

  @Option({"-i", "--instrument"})
  private void setInstruments(String instrumentsString) throws InvalidCommandException {
    dryRunIncompatible("instrument");
    this.instrumentNames = split(instrumentsString);
    // TODO: check em
  }

  @Override public ImmutableList<String> instrumentNames() {
    return instrumentNames;
  }

  // --------------------------------------------------------------------------
  // Benchmark parameters
  // --------------------------------------------------------------------------

  private Multimap<String, String> parameterValues = ArrayListMultimap.create();

  @Option("-D")
  private void addParameterSpec(String nameAndValues) throws InvalidCommandException {
    addToMultimap(nameAndValues, parameterValues);
  }

  @Override public ImmutableMultimap<String, String> benchmarkParameters() {
    return ImmutableMultimap.copyOf(parameterValues);
  }

  // --------------------------------------------------------------------------
  // JVM arguments
  // --------------------------------------------------------------------------

  private Multimap<String, String> jvmArguments = ArrayListMultimap.create();

  @Option("-J")
  private void addJvmArgumentsSpec(String nameAndValues) throws InvalidCommandException {
    dryRunIncompatible("-J");
    addToMultimap(nameAndValues, jvmArguments);
  }

  @Override public ImmutableMultimap<String, String> jvmArguments() {
    return ImmutableMultimap.copyOf(jvmArguments);
  }

  // --------------------------------------------------------------------------
  // Leftover - benchmark class name
  // --------------------------------------------------------------------------

  private Class<? /*extends Benchmark*/> benchmarkClass;

  @Override public Class<? /*extends Benchmark*/> benchmarkClass() {
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
        .add("benchmarkMethodNames", this.benchmarkMethodNames())
        .add("benchmarkParameters", this.benchmarkParameters())
        .add("calculateAggregateScore", this.calculateAggregateScore())
        .add("dryRun", this.dryRun())
        .add("instrumentNames", this.instrumentNames())
        .add("jreBaseDir", this.jreBaseDir())
        .add("jreHomeDirs", this.jreHomeDirs())
        .add("jvmArguments", this.jvmArguments())
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
    pw.println(" caliper [options...] <benchmark class name>");
    pw.println(" java <benchmark class name> [options...]");
    pw.println();
    pw.println("Options:");
    pw.println(" -h, --help        print this message");
    pw.println(" -n, --dry-run     instead of measuring, execute a single 'rep' of each");
    pw.println("                   benchmark scenario in-process (for debugging); only options");
    pw.println("                   -m, -d and -D are available in this mode");
    pw.println(" -i, --instrument  comma-separated list of measuring instruments to use;");
    pw.println("                   options include 'time', 'allocation', and 'memory_size'");
    pw.println("                   (default: time)");
    pw.println(" -w, --warmup      minimum time in seconds to warm up before each measurement;");
    pw.println("                   a positive integer (default: 10)");
    pw.println(" -m, --method      comma-separated list of benchmarks to run; 'foo' is an");
    pw.println("                   alias for 'timeFoo' (default: all found in class)");
    pw.println(" -v, --verbose     generate extremely detailed logs and include them in the");
    pw.println("                   result datafile (does not change console output)");
    pw.println(" -t, --trials      number of independent measurements to take per benchmark");
    pw.println("                   scenario; a positive integer (default: 1)");
    pw.println(" -b, --jre-base    base directory that JRE homes specified with --jre are");
    pw.println("                   considered relative to (default: paths must be absolute)");
    pw.println(" -j, --jre-home    comma-separated list of JRE home directories to test on;");
    pw.println("                   each may be either absolute or relative to --jre_base");
    pw.println("                   (default: only the JRE caliper itself is running in)");
    pw.println(" -o, --output      name of file or directory in which to store the results");
    pw.println("                   data file; if a directory a unique filename is chosen; if a");
    pw.println("                   file it is overwritten");
    pw.println(" -s, --score       also calculate and display an aggregate score for this run;");
    pw.println("                   higher is better; this score can be compared to other runs");
    pw.println("                   with the exact same arguments but otherwise means nothing");
    pw.println(" -d, --delimiter   separator used to parse --jre, --measure, --benchmark, -D");
    pw.println("                   and -J options (default: ',')");
    pw.println();
    pw.println(" -Dparam=val1,val2,... ");
    pw.println();
    pw.println(" Specifies the values to inject into the 'param' field of the benchmark class;");
    pw.println(" if multiple values or parameters are specified in this way, caliper will test");
    pw.println(" all possible combinations.");
    pw.println();
    pw.println(" -JdisplayName='jre arg list choice 1,jre arg list choice 2,...'");
    pw.println();
    pw.println(" Specifies alternate sets of JVM arguments to pass. displayName is any name");
    pw.println(" you would like this variable to appear as in reports. caliper will test all");
    pw.println(" possible combinations. Example: '-Jmemory=-Xms32m -Xmx32m,-Xms512m -Xmx512m'");
    pw.println();
  }
}
