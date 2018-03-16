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

package com.google.caliper.runner.testing;

import com.google.caliper.bridge.WorkerRequest;
import com.google.caliper.runner.worker.WorkerSpec;
import com.google.common.collect.ImmutableList;
import java.util.UUID;

/**
 * Spec for a fake worker; just specifies a main class and some VM options.
 *
 * @author Colin Decker
 */
public final class FakeWorkerSpec extends WorkerSpec {

  private final String mainClass;
  private final ImmutableList<String> vmOptions;

  private FakeWorkerSpec(Class<?> mainClass, Iterable<String> vmOptions, Iterable<String> args) {
    super(FakeWorkers.getTarget().vm(), UUID.randomUUID(), args);
    this.mainClass = mainClass.getName();
    this.vmOptions = ImmutableList.copyOf(vmOptions);
  }

  @Override
  public ImmutableList<String> additionalVmOptions() {
    return vmOptions;
  }

  @Override
  public String mainClass() {
    return mainClass;
  }

  @Override
  public WorkerRequest request() {
    return new FakeRequest();
  }

  public static Builder builder(Class<?> mainClass) {
    return new Builder(mainClass);
  }

  static final class FakeRequest implements WorkerRequest {
    @Override
    public Class<? extends WorkerRequest> type() {
      return FakeRequest.class;
    }
  }

  public static final class Builder {

    private final Class<?> mainClass;
    private ImmutableList<String> vmOptions = ImmutableList.of();
    private ImmutableList<String> args = ImmutableList.of();

    private Builder(Class<?> mainClass) {
      this.mainClass = mainClass;
    }

    public Builder setVmOptions(String... vmOptions) {
      return setVmOptions(ImmutableList.copyOf(vmOptions));
    }

    public Builder setVmOptions(Iterable<String> vmOptions) {
      this.vmOptions = ImmutableList.copyOf(vmOptions);
      return this;
    }

    public Builder setArgs(String... args) {
      this.args = ImmutableList.copyOf(args);
      return this;
    }

    public FakeWorkerSpec build() {
      return new FakeWorkerSpec(mainClass, vmOptions, args);
    }
  }
}
