/*
 * Copyright (C) 2015 Google Inc.
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

package com.google.caliper.platform.dalvik;

import static com.google.common.base.Preconditions.checkState;

import com.google.caliper.platform.Platform;
import com.google.common.base.Preconditions;
import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import com.google.common.collect.ImmutableSet;

import java.io.File;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;

/**
 * An abstraction of the Dalvik (aka Android) platform.
 *
 * <p>Although this talks about dalvik it actually works with ART too.
 */
public final class DalvikPlatform extends Platform {

  public DalvikPlatform() {
    super(Type.DALVIK);
  }

  @Override
  public File vmExecutable(File vmHome) {
    // TODO(user): Allow the 32/64 version of dalvik to be selected rather than the default
    // standard configurations of Android systems and windows.
    File bin = new File(vmHome, "bin");
    Preconditions.checkState(bin.exists() && bin.isDirectory(),
        "Could not find %s under android root %s", bin, vmHome);
    String executableName = "dalvikvm";
    File dalvikvm = new File(bin, executableName);
    if (!dalvikvm.exists() || dalvikvm.isDirectory()) {
      throw new IllegalStateException(
          String.format("Cannot find %s binary in %s", executableName, bin));
    }

    return dalvikvm;
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
  public String workerClassPath() {
    // TODO(user): Find a way to get the class path programmatically from the class loader.
    return System.getProperty("java.class.path");
  }

  @Override
  public Collection<String> inputArguments() {
    return Collections.emptyList();
  }

  @Override
  public Predicate<String> vmPropertiesToRetain() {
    return Predicates.alwaysFalse();
  }

  @Override
  public void checkVmProperties(Map<String, String> options) {
    checkState(options.isEmpty());
  }

  @Override
  public File customVmHomeDir(Map<String, String> vmGroupMap, String vmConfigName) {
    // TODO(user): Should probably use this to support specifying dalvikvm32/dalvikvm64
    // and maybe even app_process.
    throw new UnsupportedOperationException(
            "Running with a custom Dalvik VM is not currently supported");
  }
}
