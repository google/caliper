/*
 * Copyright (C) 2010 Google Inc.
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

package com.google.caliper;

import com.google.caliper.UserException.DisplayUsageException;
import com.google.caliper.UserException.IncompatibleArgumentsException;
import com.google.caliper.UserException.InvalidParameterValueException;
import com.google.caliper.UserException.MalformedParameterException;
import com.google.caliper.UserException.MultipleBenchmarkClassesException;
import com.google.caliper.UserException.NoBenchmarkClassException;
import com.google.caliper.UserException.UnrecognizedOptionException;
import com.google.common.base.Splitter;
import com.google.common.collect.Iterables;
import com.google.common.collect.Maps;
import com.google.common.collect.Iterators;
import com.google.common.collect.LinkedHashMultimap;
import com.google.common.collect.Multimap;
import com.google.common.collect.Sets;
import java.io.File;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

/**
 * Parse command line arguments for the runner and in-process runner.
 */
public final class Arguments {
  private String suiteClassName;

  /** JVMs to run in the benchmark */
  private final Set<String> userVms = Sets.newLinkedHashSet();

  /**
   * Parameter values specified by the user on the command line. Parameters with
   * no value in this multimap will get their values from the benchmark suite.
   */
  private final Multimap<String, String> userParameters = LinkedHashMultimap.create();

  private int trials = 1;
  private long warmupMillis = 3000;
  private long runMillis = 1000;
  private String timeUnit = null;
  private String instanceUnit = null;
  private String memoryUnit = null;
  private File saveResultsFile = null;
  private File uploadResultsFile = null;
  private boolean printScore = false;
  private boolean measureMemory = false;
  private MeasurementType measurementType;
  private MeasurementType primaryMeasurementType;

  private static final String defaultDelimiter = ",";

  public String getSuiteClassName() {
    return suiteClassName;
  }

  public Set<String> getUserVms() {
    return userVms;
  }

  public int getTrials() {
    return trials;
  }

  public Multimap<String, String> getUserParameters() {
    return userParameters;
  }

  public long getWarmupMillis() {
    return warmupMillis;
  }

  public long getRunMillis() {
    return runMillis;
  }

  public String getTimeUnit() {
    return timeUnit;
  }

  public String getInstanceUnit() {
    return instanceUnit;
  }

  public String getMemoryUnit() {
    return memoryUnit;
  }

  public File getSaveResultsFile() {
    return saveResultsFile;
  }

  public File getUploadResultsFile() {
    return uploadResultsFile;
  }

  public boolean printScore() {
    return printScore;
  }

  public boolean getMeasureMemory() {
    return measureMemory;
  }

  public MeasurementType getMeasurementType() {
    return measurementType;
  }

  public MeasurementType getPrimaryMeasurementType() {
    return primaryMeasurementType;
  }

  public static Arguments parse(String[] argsArray) {
    Arguments result = new Arguments();

    Iterator<String> args = Iterators.forArray(argsArray);
    String delimiter = defaultDelimiter;
    Map<String, String> userParameterStrings = Maps.newLinkedHashMap();
    String vmString = null;
    boolean standardRun = false;
    while (args.hasNext()) {
      String arg = args.next();

      if ("--help".equals(arg)) {
        throw new DisplayUsageException();
      }

      if (arg.startsWith("-D")) {
        int equalsSign = arg.indexOf('=');
        if (equalsSign == -1) {
          throw new MalformedParameterException(arg);
        }
        String name = arg.substring(2, equalsSign);
        String value = arg.substring(equalsSign + 1);
        String oldValue = userParameterStrings.put(name, value);
        if (oldValue != null) {
          throw new UserException.DuplicateParameterException(arg);
        }
        standardRun = true;
      // TODO: move warmup/run to caliperrc
      } else if ("--warmupMillis".equals(arg)) {
        result.warmupMillis = Long.parseLong(args.next());
        standardRun = true;
      } else if ("--runMillis".equals(arg)) {
        result.runMillis = Long.parseLong(args.next());
        standardRun = true;
      } else if ("--trials".equals(arg)) {
        String value = args.next();
        try {

          result.trials = Integer.parseInt(value);
          if (result.trials < 1) {
            throw new UserException.InvalidTrialsException(value);
          }
        } catch (NumberFormatException e) {
          throw new UserException.InvalidTrialsException(value);
        }
        standardRun = true;
      } else if ("--vm".equals(arg)) {
        if (vmString != null) {
          throw new UserException.DuplicateParameterException(arg);
        }
        vmString = args.next();
        standardRun = true;
      } else if ("--delimiter".equals(arg)) {
        delimiter = args.next();
        standardRun = true;
      } else if ("--timeUnit".equals(arg)) {
        result.timeUnit = args.next();
        standardRun = true;
      } else if ("--saveResults".equals(arg) || "--xmlSave".equals(arg)) {
        // TODO: unsupport legacy --xmlSave
        result.saveResultsFile = new File(args.next());
        standardRun = true;
      } else if ("--uploadResults".equals(arg)) {
        result.uploadResultsFile = new File(args.next());
      } else if ("--printScore".equals(arg)) {
        result.printScore = true;
        standardRun = true;
      } else if ("--measureMemory".equals(arg)) {
        result.measureMemory = true;
        standardRun = true;
      } else if ("--measurementType".equals(arg)) {
        String measurementType = args.next();
        try {
          result.measurementType = MeasurementType.valueOf(measurementType);
        } catch (Exception e) {
          throw new InvalidParameterValueException(arg, measurementType);
        }
        standardRun = true;
      } else if ("--primaryMeasurementType".equals(arg)) {
        String measurementType = args.next().toUpperCase();
        try {
          result.primaryMeasurementType = MeasurementType.valueOf(measurementType);
        } catch (Exception e) {
          throw new InvalidParameterValueException(arg, measurementType);
        }
        standardRun = true;
      } else if (arg.startsWith("-")) {
        throw new UnrecognizedOptionException(arg);

      } else {
        if (result.suiteClassName != null) {
          throw new MultipleBenchmarkClassesException(result.suiteClassName, arg);
        }
        result.suiteClassName = arg;
      }
    }

    Splitter delimiterSplitter = Splitter.on(delimiter);

    if (vmString != null) {
      Iterables.addAll(result.userVms, delimiterSplitter.split(vmString));
    }

    for (Map.Entry<String, String> userParameterEntry : userParameterStrings.entrySet()) {
      String name = userParameterEntry.getKey();
      result.userParameters.putAll(name, delimiterSplitter.split(userParameterEntry.getValue()));
    }

    if (standardRun && result.uploadResultsFile != null) {
      throw new IncompatibleArgumentsException("--uploadResults");
    }

    if (result.suiteClassName == null && result.uploadResultsFile == null) {
      throw new NoBenchmarkClassException();
    }

    if (result.primaryMeasurementType != null
        && result.primaryMeasurementType != MeasurementType.TIME && !result.measureMemory) {
      throw new IncompatibleArgumentsException(
          "--primaryMeasurementType " + result.primaryMeasurementType.toString().toLowerCase());
    }

    return result;
  }

  public static void printUsage() {
    System.out.println();
    System.out.println("Usage: Runner [OPTIONS...] <benchmark>");
    System.out.println();
    System.out.println("  <benchmark>: a benchmark class or suite");
    System.out.println();
    System.out.println("OPTIONS");
    System.out.println();
    System.out.println("  -D<param>=<value>: fix a benchmark parameter to a given value.");
    System.out.println("        Multiple values can be supplied by separating them with the");
    System.out.println("        delimiter specified in the --delimiter argument.");
    System.out.println();
    System.out.println("        For example: \"-Dfoo=bar,baz,bat\"");
    System.out.println();
    System.out.println("        \"benchmark\" is a special parameter that can be used to specify");
    System.out.println("        which benchmark methods to run. For example, if a benchmark has");
    System.out.println("        the method \"timeFoo\", it can be run alone by using");
    System.out.println("        \"-Dbenchmark=Foo\". \"benchmark\" also accepts a delimiter");
    System.out.println("        separated list of methods to run.");
    System.out.println();
    System.out.println("  --delimiter <delimiter>: character or string to use as a delimiter");
    System.out.println("        for parameter and vm values.");
    System.out.println("        Default: \"" + defaultDelimiter + "\"");
    System.out.println();
    System.out.println("  --warmupMillis <millis>: duration to warmup each benchmark");
    System.out.println();
    System.out.println("  --runMillis <millis>: duration to execute each benchmark");
    System.out.println();
    System.out.println("  --measureMemory: measure the number of allocations done and the amount of");
    System.out.println("        memory used by invocations of the benchmark.");
    System.out.println("        Default: off");
    System.out.println();
    System.out.println("  --vm <vm>: executable to test benchmark on. Multiple VMs may be passed");
    System.out.println("        in as a list separated by the delimiter specified in the");
    System.out.println("        --delimiter argument.");
    System.out.println();
    System.out.println("  --timeUnit <unit>: unit of time to use for result. Depends on the units");
    System.out.println("        defined in the benchmark's timeUnitNames method, if defined.");
    System.out.println("        Default Options: ns, us, ms, s");
    System.out.println();
    System.out.println("  --instanceUnit <unit>: unit to use for allocation instances result.");
    System.out.println("        Depends on the units defined in the benchmark's instanceUnitNames");
    System.out.println("        method, if defined.");
    System.out.println("        Default Options: instances, K instances, M instances, B instances");
    System.out.println();
    System.out.println("  --memoryUnit <unit>: unit to use for allocation memory size result.");
    System.out.println("        Depends on the units defined in the benchmark's memoryUnitNames");
    System.out.println("        method, if defined.");
    System.out.println("        Default Options: B, KB, MB, GB");
    System.out.println();
    System.out.println("  --saveResults <file/dir>: write results to this file or directory");
    System.out.println();
    System.out.println("  --printScore: if present, also display an aggregate score for this run,");
    System.out.println("        where higher is better. This number has no particular meaning,");
    System.out.println("        but can be compared to scores from other runs that use the exact");
    System.out.println("        same arguments.");
    System.out.println();
    System.out.println("  --uploadResults <file/dir>: upload this file or directory of files");
    System.out.println("        to the web app. This argument ends Caliper early and is thus");
    System.out.println("        incompatible with all other arguments.");

    // adding new options? don't forget to update executeForked()
  }
}
