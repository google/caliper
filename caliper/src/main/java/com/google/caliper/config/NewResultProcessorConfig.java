// Copyright 2012 Google Inc. All Rights Reserved.

package com.google.caliper.config;

import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.base.Objects;
import com.google.common.collect.ImmutableMap;

import java.util.Map;

/**
 * This is the configuration passed to the {@link com.google.caliper.runner.ResultProcessor} by the
 * user.
 *
 * @author gak@google.com (Gregory Kick)
 */
public class NewResultProcessorConfig {
  private final String className;
  private final ImmutableMap<String, String> options;

  private NewResultProcessorConfig(Builder builder) {
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
    } else if (obj instanceof NewResultProcessorConfig) {
      NewResultProcessorConfig that = (NewResultProcessorConfig) obj;
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
    return Objects.toStringHelper(this)
        .add("className", className)
        .add("options", options)
        .toString();
  }

  static final class Builder {
    private String className;
    private ImmutableMap.Builder<String, String> optionsBuilder = ImmutableMap.builder();

    public Builder className(String className) {
      this.className = checkNotNull(className);
      return this;
    }

    public Builder addOption(String option, String value) {
      this.optionsBuilder.put(option, value);
      return this;
    }

    public Builder addAllOptions(Map<String, String> options) {
      this.optionsBuilder.putAll(options);
      return this;
    }

    public NewResultProcessorConfig build() {
      return new NewResultProcessorConfig(this);
    }
  }
}
