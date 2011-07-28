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

package com.google.caliper.functional;

import com.google.caliper.runner.CaliperOptions;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableSetMultimap;

public class DefaultCaliperOptions implements CaliperOptions {
  private final String benchmarkClassName;

  public DefaultCaliperOptions(String benchmarkClassName) {
    this.benchmarkClassName = benchmarkClassName;
  }

  @Override public String benchmarkClassName() {
    return benchmarkClassName;
  }

  @Override public ImmutableSet<String> benchmarkMethodNames() {
    return ImmutableSet.of();
  }

  @Override public ImmutableSet<String> vmNames() {
    return ImmutableSet.of();
  }

  @Override public ImmutableSetMultimap<String, String> userParameters() {
    return ImmutableSetMultimap.of();
  }

  @Override public ImmutableSetMultimap<String, String> vmArguments() {
    return ImmutableSetMultimap.of();
  }

  @Override public String instrumentName() {
    return "micro";
  }

  @Override public int trials() {
    return 1;
  }

  @Override public String outputFileOrDir() {
    return null;
  }

  @Override public boolean detailedLogging() {
    return false;
  }

  @Override public boolean verbose() {
    return false;
  }

  @Override public boolean calculateAggregateScore() {
    return false;
  }

  @Override public boolean dryRun() {
    return false;
  }

  @Override public String caliperRcFilename() {
    throw new UnsupportedOperationException();
  }
}
