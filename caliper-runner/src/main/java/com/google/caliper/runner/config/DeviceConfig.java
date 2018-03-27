/*
 * Copyright (C) 2018 Google Inc.
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
import com.google.common.base.Optional;
import com.google.common.collect.ImmutableMap;
import java.util.Map;

/**
 * The configuration for a device.
 *
 * <p>This class is just a simple representation of the information from the {@link CaliperConfig}.
 */
@AutoValue
public abstract class DeviceConfig {

  /** Returns a new {@link DeviceConfig} builder. */
  public static DeviceConfig.Builder builder() {
    return new AutoValue_DeviceConfig.Builder();
  }

  /** Returns the name of this device. */
  public abstract String name();

  /** Returns the type of this device. */
  public abstract DeviceType type();

  /** Returns the configuration options for this device. */
  public abstract ImmutableMap<String, String> options();

  /** Returns the value of the option with the given key. */
  public final Optional<String> option(String key) {
    return Optional.fromNullable(options().get(key));
  }

  /** Builder for {@link DeviceConfig}. */
  @AutoValue.Builder
  public interface Builder {
    /** Sets the name of the device. */
    Builder name(String name);

    /** Sets the type of the device. */
    Builder type(DeviceType type);

    /** Sets the options for the device. */
    Builder options(Map<String, String> options);

    /** Returns a builder for setting device options. */
    ImmutableMap.Builder<String, String> optionsBuilder();

    /** Builds a new {@link DeviceConfig} object. */
    DeviceConfig build();
  }
}
