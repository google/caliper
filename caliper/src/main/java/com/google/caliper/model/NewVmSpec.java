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

import com.google.common.base.Objects;
import com.google.common.collect.ImmutableMap;

import java.util.Map;

import javax.annotation.concurrent.Immutable;

/**
 * A configuration of a virtual machine.
 *
 * @author gak@google.com (Gregory Kick)
 */
@Immutable
public final class NewVmSpec {
  private final ImmutableMap<String, String> properties;
  private final ImmutableMap<String, String> options;

  private NewVmSpec(Builder builder) {
    this.properties = builder.propertiesBuilder.build();
    this.options = builder.optionsBuilder.build();
  }

  public ImmutableMap<String, String> options() {
    return options;
  }

  public ImmutableMap<String, String> properties() {
    return properties;
  }

  @Override public boolean equals(Object obj) {
    if (obj == this) {
      return true;
    } else if (obj instanceof NewVmSpec) {
      NewVmSpec that = (NewVmSpec) obj;
      return this.properties.equals(that.properties)
          && this.options.equals(that.options);
    } else {
      return false;
    }
  }

  @Override public int hashCode() {
    return Objects.hashCode(properties, options);
  }

  @Override public String toString() {
    return Objects.toStringHelper(this)
        .add("properties", properties)
        .add("options", options)
        .toString();
  }

  public static final class Builder {
    private final ImmutableMap.Builder<String, String> propertiesBuilder = ImmutableMap.builder();
    private final ImmutableMap.Builder<String, String> optionsBuilder = ImmutableMap.builder();

    public Builder addOption(String optionName, String value) {
      optionsBuilder.put(optionName, value);
      return this;
    }

    public Builder addAllOptions(Map<String, String> options) {
      optionsBuilder.putAll(options);
      return this;
    }

    public Builder addProperty(String property, String value) {
      optionsBuilder.put(property, value);
      return this;
    }

    public Builder addAllProperties(Map<String, String> properties) {
      optionsBuilder.putAll(properties);
      return this;
    }

    public NewVmSpec build() {
      return new NewVmSpec(this);
    }
  }
}
