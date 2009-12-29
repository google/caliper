/*
 * Copyright (C) 2009 Google Inc.
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

/**
 * Signifies a problem that should be explained in user-friendly terms on the command line, without
 * a confusing stack trace, and optionally followed by a usage summary.
 */
@SuppressWarnings("serial") // never going to serialize these... right?
public abstract class UserException extends RuntimeException {
  private final boolean printUsage;

  protected UserException(String error, String remedy) {
    this(error, remedy, false);
  }

  protected UserException(String error, String remedy, boolean printUsage) {
    super(String.format("Error: %s%nTypical Remedy: %s", error, remedy));
    this.printUsage = printUsage;
  }

  // - - - -

  public static class NoSuchClassException extends UserException {
    public NoSuchClassException(String name) {
      super("No class named [" + name + "] was found.",
          "Check the spelling, package location, and CLASSPATH environment variable.");
    }
  }

  public static class AbstractClassException extends UserException {
    public AbstractClassException(Class<?> specifiedClass) {
      super("Class [" + specifiedClass + "] is abstract.", "Specify a concrete class.");
    }
  }

  public static class NoParameterlessConstructorException extends UserException {
    public NoParameterlessConstructorException(Class<?> specifiedClass) {
      super("Class [" + specifiedClass + "] has no parameterless constructor.",
          "Remove all constructors or add a parameterless constructor.");
    }
  }

  public static class DoesntImplementBenchmarkException extends UserException {
    public DoesntImplementBenchmarkException(Class<?> specifiedClass) {
      super("Class [" + specifiedClass + "] does not implement the " + Benchmark.class
          + " interface.", "Add 'extends " + SimpleBenchmark.class + "' to the class declaration.");
    }
  }

  public boolean shouldPrintUsage() {
    return printUsage;
  }
}
