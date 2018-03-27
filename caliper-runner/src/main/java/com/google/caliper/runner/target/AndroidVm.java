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

package com.google.caliper.runner.target;

import static com.google.common.base.Preconditions.checkArgument;

import com.google.caliper.runner.config.VmConfig;
import com.google.caliper.runner.config.VmType;
import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;

/** An Android VM, e.g. Dalvik or ART. */
public final class AndroidVm extends Vm {

  AndroidVm(VmConfig config, String classpath) {
    super(config, classpath);
    checkArgument(config.type().get().equals(VmType.ANDROID), "config must have type android");
  }

  @Override
  public VmType type() {
    return VmType.ANDROID;
  }

  @Override
  public String executable() {
    return config().executable().or("dalvikvm");
  }

  @Override
  public ImmutableSet<String> trialArgs() {
    return ImmutableSet.of();
  }

  @Override
  public ImmutableList<String> lastArgs() {
    // app_process expects a "command directory" argument; use the bin directory where the binary is
    return executable().equals("app_process")
        ? ImmutableList.of("/system/bin")
        : ImmutableList.of();
  }

  @Override
  public ImmutableList<String> classpathArgs() {
    // Unlike -cp <classpath>, this works for both dalvikvm and app_process
    return ImmutableList.of("-Djava.class.path=" + classpath());
  }

  @Override
  public Predicate<String> vmPropertiesToRetain() {
    return Predicates.alwaysFalse();
  }
}
