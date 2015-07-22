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

import java.io.PrintWriter;

/**
 *
 */
@SuppressWarnings("serial")
public class InvalidBenchmarkException extends RuntimeException {
  public InvalidBenchmarkException(String message, Object... args) {
    super(String.format(message, fixArgs(args)));
  }

  private static Object[] fixArgs(Object[] args) {
    for (int i = 0; i < args.length; i++) {
      if (args[i] instanceof Class) {
        args[i] = ((Class<?>) args[i]).getSimpleName();
      }
    }
    return args;
  }

  public void display(PrintWriter writer) {
    writer.println(getMessage());
  }
}
