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

package com.google.caliper.util;

import static com.google.common.base.Preconditions.checkNotNull;

import dagger.Module;
import dagger.Provides;

import java.io.PrintWriter;

/**
 * A module that binds {@link PrintWriter} instances for {@link Stdout} and {@link Stderr}.
 */
@Module
public final class OutputModule {
  public static OutputModule system() {
    return new OutputModule(new PrintWriter(System.out, true), new PrintWriter(System.err, true));
  }

  private final PrintWriter stdout;
  private final PrintWriter stderr;

  public OutputModule(PrintWriter stdout, PrintWriter stderr) {
    this.stdout = checkNotNull(stdout);
    this.stderr = checkNotNull(stderr);
  }

  @Provides @Stdout PrintWriter provideStdoutWriter() {
    return stdout;
  }

  @Provides @Stderr PrintWriter provideStderr() {
    return stderr;
  }
}
