/**
 * Copyright (C) 2009 Google Inc.
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

package com.google.caliper.cloud.server;

public final class BenchmarkPage {

  private final String benchmarkOwner;
  private final String benchmarkName;
  private final String packageName;
  private final String className;

  public BenchmarkPage(String benchmarkOwner, String benchmarkName) {
    this.benchmarkOwner = benchmarkOwner;
    this.benchmarkName = benchmarkName;

    int dotLocation = benchmarkName.lastIndexOf('.');
    this.packageName = benchmarkName.substring(0, dotLocation + 1);
    this.className = benchmarkName.substring(dotLocation + 1);
  }

  public String getBenchmarkOwner() {
    return benchmarkOwner;
  }

  public String getBenchmarkName() {
    return benchmarkName;
  }

  public String getPackageName() {
    return packageName;
  }

  public String getClassName() {
    return className;
  }
}
