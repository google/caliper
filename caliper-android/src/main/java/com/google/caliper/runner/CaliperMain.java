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

import com.google.caliper.config.InvalidConfigurationException;
import com.google.caliper.core.InvalidBenchmarkException;
import com.google.caliper.options.OptionsModule;
import com.google.caliper.platform.DalvikPlatform;
import com.google.caliper.platform.PlatformModule;
import com.google.caliper.util.InvalidCommandException;
import com.google.caliper.util.OutputModule;
import java.io.PrintWriter;

/**
 * Primary entry point for the caliper benchmark runner application; run with {@code --help} for
 * details. This class's only purpose is to take care of anything that's specific to command-line
 * invocation and then hand off to {@code CaliperRun}. That is, a hypothetical GUI benchmark runner
 * might still use {@code CaliperRun} but would skip using this class.
 */
public final class CaliperMain extends AbstractCaliperMain {

  private final String classpath;

  public CaliperMain(String classpath) {
    this.classpath = classpath;
  }

  public int exitlessMain(String[] args, PrintWriter stdout, PrintWriter stderr)
      throws InvalidCommandException, InvalidBenchmarkException, InvalidConfigurationException {
    return mainImpl(args, stdout, stderr);
  }

  @Override
  protected MainComponent createMainComponent(
      String[] args, PrintWriter stdout, PrintWriter stderr) {
    return DaggerDalvikMainComponent.builder()
        .optionsModule(OptionsModule.withBenchmarkClass(args))
        .outputModule(new OutputModule(stdout, stderr))
        .platformModule(new PlatformModule(new DalvikPlatform(classpath)))
        .build();
  }
}
