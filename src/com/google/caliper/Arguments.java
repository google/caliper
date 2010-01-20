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
import com.google.common.collect.Iterators;
import com.google.common.collect.LinkedHashMultimap;
import com.google.common.collect.Multimap;
import com.google.common.collect.Sets;
import java.util.Iterator;
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

  public static Arguments parse(String[] argsArray) {
    Arguments result = new Arguments();

    Iterator<String> args = Iterators.forArray(argsArray);
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
        result.userParameters.put(name, value);

      // TODO: move warmup/run to caliperrc
      } else if ("--warmupMillis".equals(arg)) {
        result.warmupMillis = Long.parseLong(args.next());

      } else if ("--runMillis".equals(arg)) {
        result.runMillis = Long.parseLong(args.next());

      } else if ("--vm".equals(arg)) {
        result.userVms.add(args.next());

      } else if (arg.startsWith("-")) {
        throw new UnrecognizedOptionException(arg);

      } else {
        if (result.suiteClassName != null) {
          throw new MultipleBenchmarkClassesException(result.suiteClassName, arg);
        }
        result.suiteClassName = arg;
      }
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
    System.out.println("        When multiple values for the same parameter are given (via");
    System.out.println("        multiple --Dx=y args), all supplied values are used.");
    System.out.println();
    System.out.println("  --warmupMillis <millis>: duration to warmup each benchmark");
    System.out.println();
    System.out.println("  --runMillis <millis>: duration to execute each benchmark");
    System.out.println();
    System.out.println("  --vm <vm>: executable to test benchmark on");

    // adding new options? don't forget to update executeForked()
  }
}
