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
 * Signifies that the user's benchmark code threw an exception.
 */
@SuppressWarnings("serial")
public class UserCodeException extends InvalidBenchmarkException {
  public UserCodeException(String message, Throwable cause) {
    super(message);
    initCause(cause);
  }

  public UserCodeException(Throwable cause) {
    this("An exception was thrown from the benchmark code", cause);
  }

  @Override public void display(PrintWriter writer) {
    printStackTrace(writer);
  }
}
