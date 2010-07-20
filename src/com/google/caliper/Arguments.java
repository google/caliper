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

  private long warmupMillis = 3000;
  private long runMillis = 1000;
  private TimeUnit timeUnit = null;
  private static final Map<String, TimeUnit> timeUnitMap = Maps.newHashMap();
  static {
    for (TimeUnit timeUnit : TimeUnit.getOptions()) {
      timeUnitMap.put(timeUnit.toString(), timeUnit);
    }
  }
  private File xmlSaveFile = null;

  private static final String defaultDelimiter = ",";

  public String getSuiteClassName() {
    return suiteClassName;
  }

  public Set<String> getUserVms() {
    return userVms;
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

  public TimeUnit getUnit() {
    return timeUnit;
  }

  public File getXmlSaveFile() {
    return xmlSaveFile;
  }

  public static Arguments parse(String[] argsArray) {
    Arguments result = new Arguments();

    Iterator<String> args = Iterators.forArray(argsArray);
    String delimiter = defaultDelimiter;
    Map<String, String> userParameterStrings = Maps.newLinkedHashMap();
    String vmString = null;
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
      // TODO: move warmup/run to caliperrc
      } else if ("--warmupMillis".equals(arg)) {
        result.warmupMillis = Long.parseLong(args.next());

      } else if ("--runMillis".equals(arg)) {
        result.runMillis = Long.parseLong(args.next());

      } else if ("--vm".equals(arg)) {
        if (vmString != null) {
          throw new UserException.DuplicateParameterException(arg);
        }
        vmString = args.next();

      } else if ("--delimiter".equals(arg)) {
        delimiter = args.next();

      } else if ("--timeUnit".equals(arg)) {
        String unit = args.next();
        result.timeUnit = timeUnitMap.get(unit);
        if (result.timeUnit == null) {
          throw new UserException.InvalidParameterValueException(arg, unit);
        }
        
      } else if ("--xmlSave".equals(arg)) {
        result.xmlSaveFile = new File(args.next());
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

    if (result.suiteClassName == null) {
      throw new NoBenchmarkClassException();
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
    System.out.println("        For example: \"-DFoo=bar,baz,bat\"");
    System.out.println();
    System.out.println("  --delimiter <delimiter>: character or string to use as a delimiter");
    System.out.println("        for parameter and vm values.");
    System.out.println("        Default: \"" + defaultDelimiter + "\"");
    System.out.println();
    System.out.println("  --warmupMillis <millis>: duration to warmup each benchmark");
    System.out.println();
    System.out.println("  --runMillis <millis>: duration to execute each benchmark");
    System.out.println();
    System.out.println("  --vm <vm>: executable to test benchmark on. Multiple VMs may be passed");
    System.out.println("        in as a list separated by the delimiter specified in the");
    System.out.println("        --delimiter argument.");
    System.out.println();
    System.out.println("  --timeUnit <timeUnit>: unit of time to use for result.");
    System.out.println("        Options: ns, us, ms, s");
    System.out.println();
    System.out.println("  --xmlSave <file/dir>: write XML results to this file or directory");

    // adding new options? don't forget to update executeForked()
  }
}
