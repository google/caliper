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

import java.util.Arrays;

/**
 * Signifies a problem that should be explained in user-friendly terms on the command line, without
 * a confusing stack trace, and optionally followed by a usage summary.
 */
@SuppressWarnings("serial") // never going to serialize these... right?
public abstract class UserException extends RuntimeException {
  protected final String error;

  protected UserException(String error) {
    this.error = error;
  }

  public abstract void display();

  // - - - -

  public abstract static class ErrorInUsageException extends UserException {
    protected ErrorInUsageException(String error) {
      super(error);
    }

    @Override public void display() {
      if (error != null) {
        System.err.println("Error: " + error);
      }
      Arguments.printUsage();
    }
  }

  public abstract static class ErrorInUserCodeException extends UserException {
    private final String remedy;

    protected ErrorInUserCodeException(String error, String remedy) {
      super(error);
      this.remedy = remedy;
    }

    @Override public void display() {
      System.err.println("Error: " + error);
      System.err.println("Typical Remedy: " + remedy);
    }
  }

  // - - - -

  // Not technically an error, but works nicely this way anyway
  public static class DisplayUsageException extends ErrorInUsageException {
    public DisplayUsageException() {
      super(null);
    }
  }

  public static class UnrecognizedOptionException extends ErrorInUsageException {
    public UnrecognizedOptionException(String arg) {
      super("Argument not recognized: " + arg);
    }
  }

  public static class NoBenchmarkClassException extends ErrorInUsageException {
    public NoBenchmarkClassException() {
      super("No benchmark class specified.");
    }
  }

  public static class MultipleBenchmarkClassesException extends ErrorInUsageException {
    public MultipleBenchmarkClassesException(String a, String b) {
      super("Multiple benchmark classes specified: " + Arrays.asList(a, b));
    }
  }

  public static class MalformedParameterException extends ErrorInUsageException {
    public MalformedParameterException(String arg) {
      super("Malformed parameter: " + arg);
    }
  }

  public static class DuplicateParameterException extends ErrorInUsageException {
    public DuplicateParameterException(String arg) {
      super("Duplicate parameter: " + arg);
    }
  }

  public static class InvalidParameterValueException extends ErrorInUsageException {
    public InvalidParameterValueException(String arg, String value) {
      super("Invalid value \"" + value + "\" for parameter: " + arg);
    }
  }

  public static class CantCustomizeInProcessVmException extends ErrorInUsageException {
    public CantCustomizeInProcessVmException() {
      super("Can't customize VM when running in process.");
    }
  }

  public static class NoSuchClassException extends ErrorInUsageException {
    public NoSuchClassException(String name) {
      super("No class named [" + name + "] was found (check CLASSPATH).");
    }
  }

  public static class AbstractBenchmarkException extends ErrorInUserCodeException {
    public AbstractBenchmarkException(Class<?> specifiedClass) {
      super("Class [" + specifiedClass.getName() + "] is abstract.", "Specify a concrete class.");
    }
  }

  public static class NoParameterlessConstructorException extends ErrorInUserCodeException {
    public NoParameterlessConstructorException(Class<?> specifiedClass) {
      super("Class [" + specifiedClass.getName() + "] has no parameterless constructor.",
          "Remove all constructors or add a parameterless constructor.");
    }
  }

  public static class DoesntImplementBenchmarkException extends ErrorInUserCodeException {
    public DoesntImplementBenchmarkException(Class<?> specifiedClass) {
      super("Class [" + specifiedClass + "] does not implement the " + Benchmark.class.getName()
          + " interface.", "Add 'extends " + SimpleBenchmark.class + "' to the class declaration.");
    }
  }

  // TODO: should remove the caliper stack frames....
  public static class ExceptionFromUserCodeException extends UserException {
    public ExceptionFromUserCodeException(Throwable t) {
      super("An exception was thrown from the benchmark code.");
      initCause(t);
    }
    @Override public void display() {
      System.err.println(error);
      getCause().printStackTrace(System.err);
    }
  }
}
