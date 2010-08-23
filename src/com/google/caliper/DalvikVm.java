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

import com.google.common.collect.Lists;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.List;

public final class DalvikVm implements Vm {
  private Process adbLogProcess;

  @Override public List<String> getVmSpecificOptions() {
    // this redirects all output to log (accessible with adb logcat). Need to do this
    // since GC information is written to the log, and cannot be redirected to standard
    // out.
    return Lists.newArrayList("-Xlog-stdio");
  }

  @Override public LogParser getLogParser(BufferedReader logReader) {
    // this implementation of Vm does not read directly from the subprocess (and instead reads
    // from its own logcat subprocess), so logReader is unused.

    if (adbLogProcess == null) {
      throw new ConfigurationException("Running dalvikvm, but no adb log process started");
    }

    return new AndroidLogParser(
        new BufferedReader(new InputStreamReader(adbLogProcess.getInputStream())));
  }

  @Override public void init() {
    // clear previous log output so we don't interpret old benchmark runs
    ProcessBuilder adbClearBuilder = new ProcessBuilder();
    List<String> adbClearCommand = adbClearBuilder.command();
    adbClearCommand.addAll(Lists.newArrayList("logcat", "-c"));
    Process adbClearProcess;
    try {
      adbClearProcess = adbClearBuilder.start();
    } catch (IOException e) {
      throw new RuntimeException("failed to clear log", e);
    }
    try {
      adbClearProcess.waitFor();
    } catch (InterruptedException e) {
      throw new RuntimeException("interrupted while waiting for log to clear", e);
    }

    // start reading the log
    ProcessBuilder adbLogBuilder = new ProcessBuilder();
    List<String> adbLogCommand = adbLogBuilder.command();
    adbLogCommand.add("logcat");
    try {
      adbLogProcess = adbLogBuilder.start();
    } catch (IOException e) {
      throw new RuntimeException("failed to start reading log", e);
    }
  }

  @Override public void cleanup() {
    if (adbLogProcess != null) {
      adbLogProcess.destroy();
    }
  }
}
