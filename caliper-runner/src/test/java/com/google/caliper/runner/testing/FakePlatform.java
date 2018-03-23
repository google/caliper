/*
 * Copyright (C) 2017 Google Inc.
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

package com.google.caliper.runner.testing;

import com.google.caliper.runner.platform.Platform;
import com.google.caliper.runner.platform.VmType;
import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import com.google.common.collect.ImmutableSet;
import java.io.File;
import java.util.Collection;
import java.util.Map;

/**
 * A fake platform to use for VMs that won't actually be run.
 *
 * @author Colin Decker
 */
public final class FakePlatform extends Platform {

  public FakePlatform() {
    this(VmType.JVM);
  }

  public FakePlatform(VmType type) {
    super(type);
  }

  @Override
  public File vmExecutable(File vmHome) {
    return null;
  }

  @Override
  public ImmutableSet<String> commonInstrumentVmArgs() {
    return ImmutableSet.of();
  }

  @Override
  public ImmutableSet<String> workerProcessArgs() {
    return ImmutableSet.of();
  }

  @Override
  protected String workerClassPath() {
    return "/fake/class/path";
  }

  @Override
  public Collection<String> inputArguments() {
    return ImmutableSet.of();
  }

  @Override
  public Predicate<String> vmPropertiesToRetain() {
    return Predicates.alwaysFalse();
  }

  @Override
  public void checkVmProperties(Map<String, String> options) {}

  @Override
  public File customVmHomeDir(Map<String, String> vmGroupMap, String vmConfigName) {
    return null;
  }
}
