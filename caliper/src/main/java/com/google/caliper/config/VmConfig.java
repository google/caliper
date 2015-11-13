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

package com.google.caliper.config;

import static com.google.common.base.Preconditions.checkNotNull;

import com.google.caliper.model.VmSpec;
import com.google.caliper.platform.Platform;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.MoreObjects;
import com.google.common.base.Objects;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;

import java.io.File;

import javax.annotation.concurrent.GuardedBy;

/**
 * This is the configuration passed to the VM by the user. This differs from the {@link VmSpec}
 * in that any number of configurations can yield the same spec (due to default flag values) and any
 * number of specs can come from a single configuration (due to
 * <a href="http://www.oracle.com/technetwork/java/ergo5-140223.html">ergonomics</a>).
 *
 * @author gak@google.com (Gregory Kick)
 */
public final class VmConfig {
  private final Platform platform;
  private final File vmHome;
  private final ImmutableList<String> options;

  @GuardedBy("this")
  private File vmExecutable;

  private VmConfig(Builder builder) {
    this.platform = builder.platform;
    this.vmHome = builder.vmHome;
    this.options = builder.optionsBuilder.build();
  }

  @VisibleForTesting
  public VmConfig(File vmHome, Iterable<String> options, File vmExecutable, Platform platform) {
    this.platform = platform;
    this.vmHome = checkNotNull(vmHome);
    this.vmExecutable = checkNotNull(vmExecutable);
    this.options = ImmutableList.copyOf(options);
  }

  public File vmHome() {
    return vmHome;
  }

  public synchronized File vmExecutable() {
    if (vmExecutable == null) {
      vmExecutable = platform.vmExecutable(vmHome);
    }
    return vmExecutable;
  }

  public ImmutableList<String> options() {
    return options;
  }

  public String platformName() {
    return platform.name();
  }

  public String workerClassPath() {
    return platform.workerClassPath();
  }

  public ImmutableSet<String> workerProcessArgs() {
    return platform.workerProcessArgs();
  }

  public ImmutableSet<String> commonInstrumentVmArgs() {
    return platform.commonInstrumentVmArgs();
  }

  @Override public boolean equals(Object obj) {
    if (obj == this) {
      return true;
    } else if (obj instanceof VmConfig) {
      VmConfig that = (VmConfig) obj;
      return this.platform.equals(that.platform)
          && this.vmHome.equals(that.vmHome)
          && this.options.equals(that.options);
    } else {
      return false;
    }
  }

  @Override public int hashCode() {
    return Objects.hashCode(platform, vmHome, options);
  }

  @Override public String toString() {
    return MoreObjects.toStringHelper(this)
        .add("platform", platform)
        .add("vmHome", vmHome)
        .add("options", options)
        .toString();
  }

  @VisibleForTesting public static final class Builder {
    private final Platform platform;
    private final File vmHome;
    private final ImmutableList.Builder<String> optionsBuilder = ImmutableList.builder();


    public Builder(Platform platform, File vmHome) {
      this.platform = checkNotNull(platform);
      this.vmHome = checkNotNull(vmHome);
    }

    public Builder addOption(String option) {
      optionsBuilder.add(option);
      return this;
    }

    public Builder addAllOptions(Iterable<String> options) {
      optionsBuilder.addAll(options);
      return this;
    }

    public VmConfig build() {
      return new VmConfig(this);
    }
  }
}
