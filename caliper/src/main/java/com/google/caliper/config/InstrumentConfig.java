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
import static com.google.common.base.Preconditions.checkState;

import com.google.caliper.model.InstrumentSpec;
import com.google.common.base.MoreObjects;
import com.google.common.base.Objects;
import com.google.common.collect.ImmutableMap;

import java.util.Map;

import javax.annotation.concurrent.Immutable;

/**
 * This is the configuration passed to the instrument by the user. This differs from the
 * {@link InstrumentSpec} in that any number of configurations can yield the same spec
 * (due to default option values).
 *
 * @author gak@google.com (Gregory Kick)
 */
@Immutable
public final class InstrumentConfig {
  private final String className;
  private final ImmutableMap<String, String> options;

  private InstrumentConfig(Builder builder) {
    this.className = builder.className;
    this.options = builder.optionsBuilder.build();
  }

  public String className() {
    return className;
  }

  public ImmutableMap<String, String> options() {
    return options;
  }

  @Override public boolean equals(Object obj) {
    if (obj == this) {
      return true;
    } else if (obj instanceof InstrumentConfig) {
      InstrumentConfig that = (InstrumentConfig) obj;
      return this.className.equals(that.className)
          && this.options.equals(that.options);
    } else {
      return false;
    }
  }

  @Override public int hashCode() {
    return Objects.hashCode(className, options);
  }

  @Override public String toString() {
    return MoreObjects.toStringHelper(this)
        .add("className", className)
        .add("options", options)
        .toString();
  }

  static final class Builder {
    private String className;
    private final ImmutableMap.Builder<String, String> optionsBuilder = ImmutableMap.builder();

    public Builder className(String className) {
      this.className = checkNotNull(className);
      return this;
    }

    public Builder instrumentClass(Class<?> insturmentClass) {
      return className(insturmentClass.getName());
    }

    public Builder addOption(String option, String value) {
      optionsBuilder.put(option, value);
      return this;
    }

    public Builder addAllOptions(Map<String, String> options) {
      optionsBuilder.putAll(options);
      return this;
    }

    public InstrumentConfig build() {
      checkState(className != null);
      return new InstrumentConfig(this);
    }
  }
}
