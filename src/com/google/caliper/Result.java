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

/**
 * Represents an invocation of a benchmark, including the run itself, as well as the environment
 * in which the run occurred.
 */
public final class Result {
  private /*final*/ Run run;
  private /*final*/ Environment environment;

  public Result(Run run, Environment environment) {
    this.run = run;
    this.environment = environment;
  }

  public Run getRun() {
    return run;
  }

  public Environment getEnvironment() {
    return environment;
  }

  private Result() {} // for gson
}
