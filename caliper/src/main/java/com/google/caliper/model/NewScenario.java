/*
 * Copyright (C) 2012 Google Inc.
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

package com.google.caliper.model;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;

import com.google.common.base.Objects;

import javax.annotation.concurrent.Immutable;

/**
 * The combination of properties whose combination, when measured with a particular instrument,
 * should produce a repeatable result
 *
 * @author gak@google.com (Gregory Kick)
 */
@Immutable
public final class NewScenario {
  private final NewHost host;
  private final NewVmSpec vmSpec;
  private final NewBenchmarkSpec benchmarkSpec;
  // TODO(gak): include data about caliper itself and the code being benchmarked

  private NewScenario(Builder builder) {
    this.host = builder.host;
    this.vmSpec = builder.vmSpec;
    this.benchmarkSpec = builder.benchmarkSpec;
  }

  public NewHost host() {
    return host;
  }

  public NewVmSpec vmSpec() {
    return vmSpec;
  }

  @Override public boolean equals(Object obj) {
    if (obj == this) {
      return true;
    } else if (obj instanceof NewScenario) {
      NewScenario that = (NewScenario) obj;
      return this.host.equals(that.host)
          && this.vmSpec.equals(that.vmSpec)
          && this.benchmarkSpec.equals(that.benchmarkSpec);
    } else {
      return false;
    }
  }

  @Override public int hashCode() {
    return Objects.hashCode(host, vmSpec, benchmarkSpec);
  }

  @Override public String toString() {
    return Objects.toStringHelper(this)
        .add("environment", host)
        .add("vmSpec", vmSpec)
        .add("benchmarkSpec", benchmarkSpec)
        .toString();
  }

  public static final class Builder {
    private NewHost host;
    private NewVmSpec vmSpec;
    private NewBenchmarkSpec benchmarkSpec;

    public Builder environment(NewHost.Builder hostBuilder) {
      return host(hostBuilder.build());
    }

    public Builder host(NewHost host) {
      this.host = checkNotNull(host);
      return this;
    }

    public Builder vmSpec(NewVmSpec.Builder vmSpecBuilder) {
      return vmSpec(vmSpecBuilder.build());
    }

    public Builder vmSpec(NewVmSpec vmSpec) {
      this.vmSpec = checkNotNull(vmSpec);
      return this;
    }

    public Builder benchmarkSpec(NewBenchmarkSpec.Builder benchmarkSpecBuilder) {
      return benchmarkSpec(benchmarkSpecBuilder.build());
    }

    public Builder benchmarkSpec(NewBenchmarkSpec benchmarkSpec) {
      this.benchmarkSpec = checkNotNull(benchmarkSpec);
      return this;
    }

    public NewScenario build() {
      checkState(host != null);
      checkState(vmSpec != null);
      checkState(benchmarkSpec != null);
      return new NewScenario(this);
    }
  }
}
