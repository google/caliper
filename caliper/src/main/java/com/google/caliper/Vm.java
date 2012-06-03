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

import com.google.common.collect.ImmutableList;

import java.io.File;
import java.util.List;

class Vm {
  public List<String> getVmSpecificOptions(MeasurementType type, Arguments arguments) {
    return ImmutableList.of();
  }

  /**
   * Returns a process builder to run this VM.
   *
   * @param vmArgs the path to the VM followed by VM arguments.
   * @param applicationArgs arguments to the target process
   */
  public ProcessBuilder newProcessBuilder(File workingDirectory, String classPath,
      ImmutableList<String> vmArgs, String className, ImmutableList<String> applicationArgs) {
    ProcessBuilder result = new ProcessBuilder();
    result.directory(workingDirectory);
    List<String> command = result.command();

    command.addAll(vmArgs);
    addClassPath(command, classPath);
    command.add(className);
    command.addAll(applicationArgs);
    return result;
  }

  private void addClassPath(List<String> command, String classPath) {
    command.add("-cp");


    command.add(classPath);
  }

}
