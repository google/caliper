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

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.base.Objects;
import com.google.common.base.Splitter;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;

import java.io.File;
import java.util.Map;

public final class VirtualMachine {
  public static VirtualMachine hostVm() {
    String home = System.getProperty("java.home");
    String executable = home + "/bin/java";
    String baseName = home.replaceFirst("/jre$", "").replaceFirst(".*/", "");
    return new VirtualMachine(baseName, home, executable, ImmutableList.<String>of());
  }

  // TODO(kevinb): all this stuff's a mess

  public static VirtualMachine from(String name, String base, Map<String, String> kvps) {
    String home = kvps.get("home");
    checkArgument(home != null);
    home = base + "/" + home;
    String relExec = Objects.firstNonNull(kvps.get("exec"), "bin/java"); // TODO: dalvik
    String args = kvps.get("args");
    ImmutableList<String> argList = Strings.isNullOrEmpty(args)
        ? ImmutableList.<String>of()
        : ImmutableList.copyOf(Splitter.onPattern("\\s+").split(args));

    return new VirtualMachine(name, home, home + "/" + relExec, argList);
  }

  final String name;
  final File home;
  final File execPath;
  final ImmutableList<String> arguments;

  public VirtualMachine(
      String name, String home, String execPath, ImmutableList<String> arguments) {
    this.name = checkNotNull(name);
    this.home = new File(checkNotNull(home));
    this.execPath = new File(checkNotNull(execPath));
    this.arguments = checkNotNull(arguments);

    // TODO: IAE?
    checkArgument(this.home.isDirectory());
    checkArgument(this.execPath.isFile());
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
