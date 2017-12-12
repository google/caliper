/*
 * Copyright (C) 2017 Google Inc.
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

import static com.google.common.base.Preconditions.checkNotNull;

import com.google.caliper.runner.config.VmConfig;
import com.google.common.collect.ImmutableList;
import java.util.UUID;

/**
 * Spec for a fake worker; just specifies a main class, some VM options and some args.
 *
 * @author Colin Decker
 */
final class FakeWorkerSpec extends WorkerSpec {

  private final String mainClass;
  private final ImmutableList<String> vmOptions;
  private final ImmutableList<String> args;

  private FakeWorkerSpec(
      UUID id, Class<?> mainClass, Iterable<String> vmOptions, Iterable<String> args) {
    super(id);
    this.mainClass = mainClass.getName();
    this.vmOptions = ImmutableList.copyOf(vmOptions);
    this.args = ImmutableList.copyOf(args);
  }

  @Override
  public ImmutableList<String> vmOptions(VmConfig config) {
    return vmOptions;
  }

  @Override
  public String mainClass() {
    return mainClass;
  }

  @Override
  public ImmutableList<String> args() {
    return args;
  }

  public static Builder builder(Class<?> mainClass) {
    return new Builder(mainClass);
  }

  static final class Builder {

    private final Class<?> mainClass;
    private UUID id = UUID.randomUUID();
    private ImmutableList<String> vmOptions = ImmutableList.of();
    private ImmutableList<String> args = ImmutableList.of();

    private Builder(Class<?> mainClass) {
      this.mainClass = mainClass;
    }

    Builder setId(UUID id) {
      this.id = checkNotNull(id);
      return this;
    }

    Builder setVmOptions(String... vmOptions) {
      return setVmOptions(ImmutableList.copyOf(vmOptions));
    }

    Builder setVmOptions(Iterable<String> vmOptions) {
      this.vmOptions = ImmutableList.copyOf(vmOptions);
      return this;
    }

    Builder setArgs(String... args) {
      return setArgs(ImmutableList.copyOf(args));
    }

    Builder setArgs(Iterable<String> args) {
      this.args = ImmutableList.copyOf(args);
      return this;
    }

    FakeWorkerSpec build() {
      return new FakeWorkerSpec(id, mainClass, vmOptions, args);
    }
  }
}
