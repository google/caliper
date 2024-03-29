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

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static java.util.concurrent.TimeUnit.MINUTES;
import static java.util.concurrent.TimeUnit.NANOSECONDS;

import com.google.caliper.runner.options.CommandLineParser.Leftovers;
import com.google.caliper.runner.options.CommandLineParser.Option;
import com.google.caliper.util.InvalidCommandException;
import com.google.caliper.util.ShortDuration;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.MoreObjects;
import com.google.common.base.Optional;
import com.google.common.base.Splitter;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableSetMultimap;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;
import com.google.common.collect.Ordering;
import java.io.File;
import java.util.List;
import java.util.Map;

/**
 * Implementation of {@link CaliperOptions} that uses {@link CommandLineParser} to parse command
 * line arguments.
 */
@VisibleForTesting
public final class ParsedOptions implements CaliperOptions {

  // TODO(cgdecker): Consider making this a @Module itself

  /**
   * Parses the given {@code args} into a {@link ParsedOptions} object. If {@code
   * requireBenchmarkClassName} is true, fail if the args do not include a benchmark class name.
   *
   * @throws InvalidCommandException if the args cannot be parsed into a valid set of Caliper
   *     options
   */
  public static ParsedOptions from(String[] args, boolean requireBenchmarkClassName)
      throws InvalidCommandException {
    ParsedOptions options = new ParsedOptions(args, requireBenchmarkClassName);

    CommandLineParser<ParsedOptions> parser = CommandLineParser.forClass(ParsedOptions.class);
    try {
      parser.parseAndInject(args, options);
    } catch (InvalidCommandException e) {
      e.setUsage(USAGE);
      throw e;
    }
    return options;
  }

  /** The list of args that were passed in to create this object. */
  private final ImmutableList<String> args;

  /**
   * True if the benchmark class name is expected as the last argument, false if it is not allowed.
   */
  private final boolean requireBenchmarkClassName;

  private ParsedOptions(String[] args, boolean requireBenchmarkClassName) {
    this.args = ImmutableList.copyOf(args);
    this.requireBenchmarkClassName = requireBenchmarkClassName;
  }

  @Override
  public ImmutableList<String> allArgs() {
    return args;
  }

  // --------------------------------------------------------------------------
  // Dry run -- simple boolean, needs to be checked in some methods
  // --------------------------------------------------------------------------

  @Option({"-n", "--dry-run"})
  private boolean dryRun;

  @Override
  public boolean dryRun() {
    return dryRun;
  }

  private void dryRunIncompatible(String optionName) throws InvalidCommandException {
    // This only works because CLP does field injection before method injection
    if (dryRun) {
      throw new InvalidCommandException("Option not available in dry-run mode: " + optionName);
    }
  }

  // --------------------------------------------------------------------------
  // Local port
  // --------------------------------------------------------------------------

  private int localPort = 0;  /* bind to any available port */

  @Option({"-L", "--local_port"})
  private void setLocalPort(int localPort) {
    if (localPort < 1) {
      throw new InvalidCommandException("local_port must be at least 1: " + localPort);
    }
    this.localPort = localPort;
  }

  @Override
  public int localPort() {
    return localPort;
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

  @Override
  public ImmutableSet<String> benchmarkMethodNames() {
    return benchmarkNames;
  }

  // --------------------------------------------------------------------------
  // Print configuration?
  // --------------------------------------------------------------------------

  @Option({"-p", "--print-config"})
  private boolean printConfiguration = false;

  @Override
  public boolean printConfiguration() {
    return printConfiguration;
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

  @Override
  public int trialsPerScenario() {
    return trials;
  }

  // --------------------------------------------------------------------------
  // Time limit
  // --------------------------------------------------------------------------

  private ShortDuration timeLimit = ShortDuration.of(5, MINUTES);

  @Option({"-l", "--time-limit"})
  private void setTimeLimit(String timeLimitString) throws InvalidCommandException {
    try {
      this.timeLimit = ShortDuration.valueOf(timeLimitString);
      if (this.timeLimit.equals(ShortDuration.zero())) {
        this.timeLimit = ShortDuration.of(Long.MAX_VALUE, NANOSECONDS);
      }
    } catch (IllegalArgumentException e) {
      throw new InvalidCommandException("Invalid time limit: " + timeLimitString);
    }
  }

  @Override
  public ShortDuration timeLimit() {
    return timeLimit;
  }

  // --------------------------------------------------------------------------
  // Run name
  // --------------------------------------------------------------------------

  private String runName = "";

  @Option({"-r", "--run-name"})
  private void setRunName(String runName) {
    this.runName = checkNotNull(runName);
  }

  @Override
  public String runName() {
    return runName;
  }

  // --------------------------------------------------------------------------
  // Device
  // --------------------------------------------------------------------------

  private String deviceName = "default";

  @Option({"-e", "--device"})
  private void setDeviceName(String deviceName) {
    if (deviceName.indexOf('@') != -1) {
      throw new InvalidCommandException("device names may not contain '@': %s", deviceName);
    }
    this.deviceName = deviceName;
  }

  @Override
  public String deviceName() {
    return deviceName;
  }

  // --------------------------------------------------------------------------
  // VM specifications
  // --------------------------------------------------------------------------

  private ImmutableSet<String> vmNames = ImmutableSet.of();

  @Option({"-m", "--vm"})
  private void setVms(String vmsString) throws InvalidCommandException {
    dryRunIncompatible("vm");
    vmNames = split(vmsString);
    for (String name : vmNames) {
      if (name.indexOf('@') != -1) {
        throw new InvalidCommandException("VM names may not contain '@': %s", name);
      }
    }
  }

  @Override
  public ImmutableSet<String> vmNames() {
    return vmNames;
  }

  // --------------------------------------------------------------------------
  // Measuring instruments to use
  // --------------------------------------------------------------------------

  // If no instrument options are provided, the default set of instruments from config will be used.
  private ImmutableSet<String> instrumentNames = ImmutableSet.of();

  @Option({"-i", "--instrument"})
  private void setInstruments(String instrumentsString) {
    instrumentNames = split(instrumentsString);
  }

  @Override
  public ImmutableSet<String> instrumentNames() {
    return instrumentNames;
  }

  // --------------------------------------------------------------------------
  // Benchmark parameters
  // --------------------------------------------------------------------------

  private Multimap<String, String> mutableUserParameters = ArrayListMultimap.create();

  @Option("-D")
  private void addParameterSpec(String nameAndValues) throws InvalidCommandException {
    addToMultimap(nameAndValues, mutableUserParameters);
  }

  @Override
  public ImmutableSetMultimap<String, String> userParameters() {
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

  @Override
  public ImmutableSetMultimap<String, String> vmArguments() {
    // de-dup values, but keep in order
    return new ImmutableSetMultimap.Builder<String, String>()
        .orderKeysBy(Ordering.natural())
        .putAll(mutableVmArguments)
        .build();
  }

  // --------------------------------------------------------------------------
  // VM arguments
  // --------------------------------------------------------------------------

  private final Map<String, String> mutableConfigProperties = Maps.newHashMap();

  @Option("-C")
  private void addConfigProperty(String nameAndValue) throws InvalidCommandException {
    List<String> tokens = splitProperty(nameAndValue);
    mutableConfigProperties.put(tokens.get(0), tokens.get(1));
  }

  @Override
  public ImmutableMap<String, String> configProperties() {
    return ImmutableMap.copyOf(mutableConfigProperties);
  }

  // --------------------------------------------------------------------------
  // Worker classpath(s)
  // --------------------------------------------------------------------------

  private final Map<String, String> workerClasspaths = Maps.newHashMap();

  @Option("--worker-classpath")
  private void setDefaultWorkerClasspath(String classpath) {
    putWorkerClasspath("android", classpath);
    putWorkerClasspath("jvm", classpath);
  }

  @Option("--worker-classpath-android")
  private void setAndroidWorkerClasspath(String classpath) {
    putWorkerClasspath("android", classpath);
  }

  @Option("--worker-classpath-jvm")
  private void setJvmWorkerClasspath(String classpath) {
    putWorkerClasspath("jvm", classpath);
  }

  private void putWorkerClasspath(String key, String classpath) {
    if (workerClasspaths.containsKey(key)) {
      throw new InvalidCommandException(
          "Provide either --worker-classpath or --worker-classpath-<vmtype>; not both");
    }
    workerClasspaths.put(key, classpath);
  }

  private static final ImmutableSet<String> VALID_VM_TYPES = ImmutableSet.of("jvm", "android");

  @Override
  public Optional<String> workerClasspath(String vmType) {
    vmType = vmType.toLowerCase(); // so passing VmType.name() works
    checkArgument(VALID_VM_TYPES.contains(vmType), "invalid VM type: %s", vmType);
    return Optional.fromNullable(workerClasspaths.get(vmType));
  }

  // --------------------------------------------------------------------------
  // Location of .caliper
  // --------------------------------------------------------------------------

  private File caliperDirectory = new File(System.getProperty("user.home"), ".caliper");

  @Option({"--directory"})
  private void setCaliperDirectory(String path) {
    caliperDirectory = new File(path);
  }

  @Override
  public File caliperDirectory() {
    return caliperDirectory;
  }

  // --------------------------------------------------------------------------
  // Location of config.properties
  // --------------------------------------------------------------------------

  private Optional<File> caliperConfigFile = Optional.absent();

  @Option({"-c", "--config"})
  private void setCaliperConfigFile(String filename) {
    caliperConfigFile = Optional.of(new File(filename));
  }

  @Override
  public File caliperConfigFile() {
    return caliperConfigFile.or(new File(caliperDirectory, "config.properties"));
  }

  // --------------------------------------------------------------------------
  // ADB args, for use by AdbDevice
  // --------------------------------------------------------------------------

  private ImmutableList<String> adbArgs = ImmutableList.of();

  @Option("--adb-args")
  private void setAdbArgs(String args) {
    // Don't use the delimiter that's used for other options here; it's "," by default, which
    // seems surprising for an option that is a series of command line args.
    adbArgs = ImmutableList.copyOf(Splitter.on(' ').split(args));
  }

  @Override
  public ImmutableList<String> adbArgs() {
    return adbArgs;
  }

  // --------------------------------------------------------------------------
  // Whether to keep the Android app installed after a run
  // --------------------------------------------------------------------------

  private boolean keepAndroidApp = false;

  @Option("--keep-android-app")
  private void setKeepAndroidApp(boolean keepAndroidApp) {
    this.keepAndroidApp = keepAndroidApp;
  }

  @Override
  public boolean keepAndroidApp() {
    return keepAndroidApp;
  }

  // --------------------------------------------------------------------------
  // Miscellaneous
  // --------------------------------------------------------------------------

  @Option("--print-worker-log")
  private boolean printWorkerLog = false;

  @Override
  public boolean printWorkerLog() {
    return printWorkerLog;
  }

  // --------------------------------------------------------------------------
  // Leftover - benchmark class name
  // --------------------------------------------------------------------------

  private String benchmarkClassName;

  @Leftovers
  private void setLeftovers(ImmutableList<String> leftovers) throws InvalidCommandException {
    if (requireBenchmarkClassName) {
      if (leftovers.isEmpty()) {
        throw new InvalidCommandException("No benchmark class specified");
      }
      if (leftovers.size() > 1) {
        throw new InvalidCommandException("Extra stuff, expected only class name: " + leftovers);
      }
      this.benchmarkClassName = leftovers.get(0);
    } else {
      if (!leftovers.isEmpty()) {
        throw new InvalidCommandException(
            "Extra stuff, did not expect non-option arguments: " + leftovers);
      }
    }
  }

  @Override
  public String benchmarkClassName() {
    return benchmarkClassName;
  }

  // --------------------------------------------------------------------------
  // Helper methods
  // --------------------------------------------------------------------------

  private static List<String> splitProperty(String propertyString) throws InvalidCommandException {
    List<String> tokens = ImmutableList.copyOf(Splitter.on('=').limit(2).split(propertyString));
    if (tokens.size() != 2) {
      throw new InvalidCommandException("no '=' found in: " + propertyString);
    }
    return tokens;
  }

  private void addToMultimap(String nameAndValues, Multimap<String, String> multimap)
      throws InvalidCommandException {
    List<String> tokens = splitProperty(nameAndValues);
    String name = tokens.get(0);
    String values = tokens.get(1);

    if (multimap.containsKey(name)) {
      throw new InvalidCommandException("multiple parameter sets for: " + name);
    }
    multimap.putAll(name, split(values));
  }

  @Override
  public String toString() {
    return MoreObjects.toStringHelper(this)
        .add("benchmarkClassName", this.benchmarkClassName())
        .add("benchmarkMethodNames", this.benchmarkMethodNames())
        .add("benchmarkParameters", this.userParameters())
        .add("dryRun", this.dryRun())
        .add("instrumentNames", this.instrumentNames())
        .add("vms", this.vmNames())
        .add("vmArguments", this.vmArguments())
        .add("trials", this.trialsPerScenario())
        .add("printConfig", this.printConfiguration())
        .add("delimiter", this.delimiter)
        .add("caliperConfigFile", this.caliperConfigFile)
        .toString();
  }

  // --------------------------------------------------------------------------
  // Usage
  // --------------------------------------------------------------------------

  // TODO(kevinb): kinda nice if CommandLineParser could autogenerate most of this...
  // TODO(kevinb): a test could actually check that we don't exceed 79 columns.
  private static final ImmutableList<String> USAGE =
      ImmutableList.of(
          "Usage:",
          " java com.google.caliper.runner.CaliperMain <benchmark_class_name> [options...]",
          "",
          "Options:",
          " -h, --help         print this message",
          " -n, --dry-run      instead of measuring, execute a single rep for each scenario",
          "                    in-process",
          " -b, --benchmark    comma-separated list of benchmark methods to run; 'foo' is",
          "                    an alias for 'timeFoo' (default: all found in class)",
          " -e, --device       name of the configured device to run the benchmarks on; currently",
          "                    only 'local' is supported (default: local)",
          " -m, --vm           comma-separated list of VMs to test on; possible values are",
          "                    configured in Caliper's configuration file (default:",
          "                    whichever VM caliper itself is running in, only)",
          " -i, --instrument   comma-separated list of measuring instruments to use; possible ",
          "                    values are configured in Caliper's configuration file ",
          "                    (default: the default set of instruments from the global or user ",
          "                    configuration file)",
          " -t, --trials       number of independent trials to perform per benchmark scenario; ",
          "                    a positive integer (default: 1)",
          " -l, --time-limit   maximum length of time allowed for a single trial; use 0 to allow ",
          "                    trials to run indefinitely. (default: 30s) ",
          " -r, --run-name     a user-friendly string used to identify the run",
          " -p, --print-config print the effective configuration that will be used by Caliper",
          " -d, --delimiter    separator used in options that take multiple values (default: ',')",
          " -c, --config       location of Caliper's configuration file (default:",
          "                    $HOME/.caliper/config.properties)",
          " --directory        location of Caliper's configuration and data directory ",
          "                    (default: $HOME/.caliper)",
          " --worker-classpath[-<vmtype>]",
          "                    the classpath to use for benchmark workers with the specified",
          "                    <vmtype> (e.g. --worker-classpath-jvm). The worker classpath must ",
          "                    include the benchmark class to be run, its dependencies, and the ",
          "                    Caliper worker code. If the run only targets a single VM type, the ",
          "                    '-<vmtype>' can be dropped. For now, if no worker classpath is ",
          "                    provided and the benchmark is running on the local device, the ",
          "                    classpath of the runner process may be used as a default. This is ",
          "                    for compatibility reasons and may not be supported in the future.",
          " --adb-args         additional arguments to pass to adb when using an adb-connected ",
          "                    device. This is primarily intended for use with things like -P ",
          "                    (setting the port to use for the adb instance); while it could ",
          "                    also be used for args such as -d that select the device to use, ",
          "                    prefer using --device for that.",
          " --keep-android-app whether to keep the Android app installed after a run.",
          " --print-worker-log if an error occurs in a worker, print the log for that worker ",
          "                    rather than the path to the log file; primarily for use in tests ",
          "                    when running in a CI environment where the log files may not be ",
          "                    easily accessible.",
          "",
          " -Dparam=val1,val2,...",
          "     Specifies the values to inject into the 'param' field of the benchmark",
          "     class; if multiple values or parameters are specified in this way, caliper",
          "     will try all possible combinations.",
          "",
          // commented out until this flag is fixed
          // " -JdisplayName='vm arg list choice 1,vm arg list choice 2,...'",
          // "     Specifies alternate sets of VM arguments to pass. As with any variable,",
          // "     caliper will test all possible combinations. Example:",
          // "     -Jmemory='-Xms32m -Xmx32m,-Xms512m -Xmx512m'",
          // "",
          " -CconfigProperty=value",
          "     Specifies a value for any property that could otherwise be specified in ",
          "     $HOME/.caliper/config.properties. Properties specified on the command line",
          "     will override those specified in the file.",
          "",
          "See https://github.com/google/caliper/wiki/CommandLineOptions for more details.",
          "");
}
