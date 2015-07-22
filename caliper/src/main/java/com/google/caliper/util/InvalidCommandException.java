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

package com.google.caliper.util;

import com.google.common.collect.ImmutableList;

import java.io.PrintWriter;
import java.util.List;

/**
 * Exception that signifies that the <i>user</i> has given an invalid argument string.
 */
@SuppressWarnings("serial") // who would serialize a command-line parsing error?
public class InvalidCommandException extends RuntimeException {
  private ImmutableList<String> usage;

  public InvalidCommandException(String message, Object... args) {
    super(String.format(message, args));
  }

  public void setUsage(List<String> usage) {
    this.usage = ImmutableList.copyOf(usage);
  }

  public void display(PrintWriter writer) {
    writer.println(getMessage());
    if (usage != null) {
      writer.println();
      displayUsage(writer);
    }
  }

  protected final void displayUsage(PrintWriter writer) {
    for (String line : usage) {
      writer.println(line);
    }
  }

  public int exitCode() {
    return 1;
  }
}
