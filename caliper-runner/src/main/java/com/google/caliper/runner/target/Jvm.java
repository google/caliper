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

import com.google.auto.value.AutoValue;
import com.google.caliper.runner.config.VmConfig;
import com.google.caliper.runner.config.VmType;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Predicate;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;

/** A standard Java Virtual Machine. */
@AutoValue
public abstract class Jvm extends Vm {

  @VisibleForTesting
  public static final ImmutableSet<String> TRIAL_VM_ARGS =
      ImmutableSet.of(
          // do compilation serially
          "-Xbatch",
          // make sure compilation doesn't run in parallel with itself
          "-XX:CICompilerCount=1",
          // CICompilerCount=1 doesn't work otherwise, on Oracle JDK8 anyway
          "-XX:-TieredCompilation",
          // ensure the parallel garbage collector
          "-XX:+UseParallelGC",
          // generate classes or don't, but do it immediately
          "-Dsun.reflect.inflationThreshold=0",
          // Make the VM print various things instruments may want to look at
          "-XX:+PrintFlagsFinal",
          "-XX:+PrintCompilation",
          "-XX:+PrintGC");

  private static final Predicate<String> PROPERTIES_TO_RETAIN =
      new Predicate<String>() {
        @Override
        public boolean apply(String input) {
          return input.startsWith("java.vm")
              || input.startsWith("java.runtime")
              || input.equals("java.version")
              || input.equals("java.vendor")
              || input.equals("sun.reflect.noInflation")
              || input.equals("sun.reflect.inflationThreshold");
        }
      };

  /** Creates a new {@link Jvm} for the given configuration. */
  public static Jvm create(VmConfig config, String classpath) {
    return new AutoValue_Jvm(VmType.JVM, config, classpath);
  }

  Jvm() {}

  @Override
  public String executable() {
    return config().executable().or("java");
  }

  @Override
  public ImmutableSet<String> trialArgs() {
    return TRIAL_VM_ARGS;
  }

  @Override
  public ImmutableList<String> classpathArgs() {
    return ImmutableList.of("-cp", classpath());
  }

  @Override
  protected ImmutableList<String> lastArgs() {
    return ImmutableList.of();
  }

  @Override
  public Predicate<String> vmPropertiesToRetain() {
    return PROPERTIES_TO_RETAIN;
  }
}
