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

package com.google.caliper.runner.config;

import com.google.auto.value.AutoValue;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;

/**
 * The configuration for a VM.
 *
 * <p>This class is just a simple representation of the information from the {@link CaliperConfig}.
 * Optional fields not set there will be absent in the {@link VmConfig} instance. Other classes are
 * responsible for handling optional fields that are unset; for example, if the type isn't set, it
 * comes from the default VM type set for the device.
 */
@AutoValue
public abstract class VmConfig {

  /** Returns a new builder for {@link VmConfig} instances. */
  public static Builder builder() {
    return new AutoValue_VmConfig.Builder();
  }

  /** The name of this VM configuration. */
  public abstract String name();

  /** The type of the VM. */
  public abstract Optional<VmType> type();

  /**
   * The path, possibly relative, to a directory under which the VM executable can be found, either
   * directly or in a "bin/" subdirectory.
   */
  public abstract Optional<String> home();

  /** The name of or relative path to the VM executable file. */
  public abstract Optional<String> executable();

  /** The arguments specific to this VM configuration. */
  public abstract ImmutableList<String> args();

  /** Builder for creating {@link VmConfig} objects. */
  @AutoValue.Builder
  @VisibleForTesting
  public abstract static class Builder {
    /** Sets the name of the VM configuration. */
    public abstract Builder name(String name);

    /** Sets the type of the VM. */
    public abstract Builder type(VmType type);

    /** Sets the path for the VM home directory. */
    public abstract Builder home(String home);

    /** Sets the path for the VM executable. */
    public abstract Builder executable(String executable);

    /** Returns a builder for adding VM args. */
    public abstract ImmutableList.Builder<String> argsBuilder();

    /** Adds the given VM arg to the configuration. */
    public Builder addArg(String arg) {
      argsBuilder().add(arg);
      return this;
    }

    /** Adds all of the given VM args to the configuration. */
    public Builder addAllArgs(Iterable<String> args) {
      argsBuilder().addAll(args);
      return this;
    }

    /** Creates a new {@link VmConfig}. */
    public abstract VmConfig build();
  }
}
