/*
 * Copyright (C) 2013 Google Inc.
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

import static com.google.common.base.Preconditions.checkState;

import com.google.caliper.config.InvalidConfigurationException;
import com.google.caliper.model.Trial;
import com.google.caliper.util.InvalidCommandException;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.common.io.Files;

import org.junit.rules.TestWatcher;
import org.junit.runner.Description;

import java.io.File;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Arrays;
import java.util.List;

/**
 * A {@link TestWatcher} that can be used to configure a run of caliper.
 * 
 * <p>Provides common test configuration utilities and redirects output to a buffer and only dumps
 * it during a failure.
 * 
 * <p>TODO(lukes,gak): This is a bad name since it isn't just watching the tests, it is helping you
 * run the tests.
 */
public final class CaliperTestWatcher extends TestWatcher {
  // N.B. StringWriter is internally synchronized and is safe to write to from multiple threads.
  private StringWriter stdout;
  private final StringWriter stderr = new StringWriter();
  private File workerOutput;

  private String instrument;
  private Class<?> benchmarkClass;
  private List<String> extraOptions = Lists.newArrayList();

  CaliperTestWatcher forBenchmark(Class<?> benchmarkClass) {
    this.benchmarkClass = benchmarkClass;
    return this;
  }
  
  CaliperTestWatcher instrument(String instrument) {
    this.instrument = instrument;
    return this;
  }
  
  CaliperTestWatcher options(String... extraOptions) {
    this.extraOptions = Arrays.asList(extraOptions);
    return this;
  }
  
  void run() throws InvalidCommandException, InvalidBenchmarkException, 
      InvalidConfigurationException {
    checkState(benchmarkClass != null, "You must configure a benchmark!");
    workerOutput = Files.createTempDir();
    // configure a custom dir so the files aren't deleted when CaliperMain returns
    List<String> options = Lists.newArrayList(
        "-Cworker.output=" + workerOutput.getPath(),
        "-Cresults.file.class=",
        "-Cresults.upload.class=" + InMemoryResultsUploader.class.getName());
    if (instrument != null) {
      options.add("-i");
      options.add(instrument);
    }
    options.addAll(extraOptions);
    options.add(benchmarkClass.getName());
    this.stdout = new StringWriter();
    CaliperMain.exitlessMain(
        options.toArray(new String[0]),
        new PrintWriter(stdout,  true),
        new PrintWriter(stderr,  true));
  }

  @Override protected void finished(Description description) {
    if (workerOutput != null) {
      for (File f : workerOutput.listFiles()) {
        f.delete();
      }
      workerOutput.delete();
    }
  }
  
  @Override protected void failed(Throwable e, Description description) {
    // don't log if run was never called.
    if (stdout != null) {
      System.err.println("Caliper failed with the following output (stdout):\n"
          + stdout.toString() + "stderr:\n" + stderr.toString());
    }
  }
  
  ImmutableList<Trial> trials() {
    return InMemoryResultsUploader.trials();
  }

  public StringWriter getStderr() {
    return stderr;
  }
  
  public StringWriter getStdout() {
    return stdout;
  }
  
  File workerOutputDirectory() {
    return workerOutput;
  }
}
