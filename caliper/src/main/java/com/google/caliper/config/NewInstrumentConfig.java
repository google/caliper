// Copyright 2012 Google Inc. All Rights Reserved.

package com.google.caliper.config;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;

import com.google.caliper.model.NewInstrumentSpec;
import com.google.common.base.Objects;
import com.google.common.collect.ImmutableMap;

import java.util.Map;

import javax.annotation.concurrent.Immutable;

/**
 * This is the configuration passed to the instrument by the user. This differs from the
 * {@link NewInstrumentSpec} in that any number of configurations can yield the same spec
 * (due to default option values).
 *
 * @author gak@google.com (Gregory Kick)
 */
@Immutable
public final class NewInstrumentConfig {
  private final String className;
  private final ImmutableMap<String, String> options;

  private NewInstrumentConfig(Builder builder) {
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
    } else if (obj instanceof NewInstrumentConfig) {
      NewInstrumentConfig that = (NewInstrumentConfig) obj;
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

    public NewInstrumentConfig build() {
      checkState(className != null);
      return new NewInstrumentConfig(this);
    }
  }
}
