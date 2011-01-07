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
import java.util.ArrayList;
import java.util.List;

/**
 * The dalvikvm run on Android devices via the app_process executable.
 */
final class DalvikVm extends Vm {

  public static boolean isDalvikVm() {
    return "Dalvik".equals(System.getProperty("java.vm.name"));
  }

  public static String vmName() {
    return "app_process";
  }

  @Override public List<String> getVmSpecificOptions(MeasurementType type, Arguments arguments) {
    if (!arguments.getCaptureVmLog()) {
      return ImmutableList.of();
    }

    List<String> result = new ArrayList<String>();
    if (arguments.getCaptureVmLog()) {
      // TODO: currently GC goes to logcat.
      // result.add("-verbose:gc");
    }
    return result;
  }

  @Override public ProcessBuilder newProcessBuilder(File workingDirectory, String classPath,
      ImmutableList<String> vmArgs, String className, ImmutableList<String> applicationArgs) {
    ProcessBuilder result = new ProcessBuilder();
    result.directory(workingDirectory);
    result.command().addAll(vmArgs);
    result.command().add("-Djava.class.path=" + classPath);
    result.command().add(workingDirectory.getPath());
    result.command().add(className);
    result.command().addAll(applicationArgs);
    return result;
  }
}
