/*
 * Copyright (C) 2011 Google Inc.
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

package com.google.caliper.runner;

import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.base.Objects;
import com.google.common.collect.ImmutableList;

import java.io.File;

public final class VirtualMachine {
  public static VirtualMachine hostVm() {
    String home = System.getProperty("java.home");
    File executable = new File(home, "bin/java");
    String baseName = home.replaceFirst("/jre$", "").replaceFirst(".*/", "");
    return new VirtualMachine(baseName, executable, ImmutableList.<String>of());
  }

  final String name;
  final File execPath;
  final ImmutableList<String> arguments;

  public VirtualMachine(String name, File execPath, ImmutableList<String> arguments) {
    this.name = checkNotNull(name);
    this.execPath = checkNotNull(execPath);
    this.arguments = checkNotNull(arguments);
  }

  // TODO(kevinb): ImmutableMap<String, String> detectProperties() {}

  @Override public boolean equals(Object object) {
    if (object instanceof VirtualMachine) {
      VirtualMachine that = (VirtualMachine) object;
      return this.name.equals(that.name)
          && this.execPath.equals(that.execPath)
          && this.arguments.equals(that.arguments);
    }
    return false;
  }

  @Override public int hashCode() {
    return Objects.hashCode(name, execPath, arguments);
  }

  @Override public String toString() {
    return name;
  }
}
