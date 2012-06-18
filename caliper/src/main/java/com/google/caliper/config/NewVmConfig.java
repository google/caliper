// Copyright 2012 Google Inc. All Rights Reserved.

package com.google.caliper.config;

import static com.google.common.base.Preconditions.checkNotNull;

import com.google.caliper.model.NewVmSpec;
import com.google.common.base.Objects;
import com.google.common.collect.ImmutableList;

import java.io.File;
import java.lang.management.ManagementFactory;

import javax.annotation.concurrent.Immutable;

/**
 * This is the configuration passed to the VM by the user. This differs from the {@link NewVmSpec}
 * in that any number of configurations can yield the same spec (due to default flag values) and any
 * number of specs can come from a single configuration (due to
 * <a href="http://www.oracle.com/technetwork/java/ergo5-140223.html">ergonomics</a>).
 *
 * @author gak@google.com (Gregory Kick)
 */
@Immutable
public final class NewVmConfig {
  /** Returns the configuration of the current host JVM (including the flags used to create it). */
  public static NewVmConfig hostVmConfig() {
    return new Builder(new File(System.getProperty("java.home")))
        .addAllOptions(ManagementFactory.getRuntimeMXBean().getInputArguments())
        .build();
  }

  private final File javaHome;
  private final ImmutableList<String> options;

  private NewVmConfig(Builder builder) {
    this.javaHome = builder.javaHome;
    this.options = builder.optionsBuilder.build();
  }

  public File javaHome() {
    return javaHome;
  }

  public File javaExecutable() {
    // TODO(gak): support other platforms
    return new File(javaHome, "bin/java");
  }

  public ImmutableList<String> options() {
    return options;
  }

  @Override public boolean equals(Object obj) {
    if (obj == this) {
      return true;
    } else if (obj instanceof NewVmConfig) {
      NewVmConfig that = (NewVmConfig) obj;
      return this.javaHome.equals(that.javaHome)
          && this.options.equals(that.options);
    } else {
      return false;
    }
  }

  @Override public int hashCode() {
    return Objects.hashCode(javaHome, options);
  }

  @Override public String toString() {
    return Objects.toStringHelper(this)
        .add("javaHome", javaHome)
        .add("options", options)
        .toString();
  }

  static final class Builder {
    private final File javaHome;
    private final ImmutableList.Builder<String> optionsBuilder = ImmutableList.builder();

    public Builder(File javaHome) {
      this.javaHome = checkNotNull(javaHome);
    }

    public Builder addOption(String option) {
      optionsBuilder.add(option);
      return this;
    }

    public Builder addAllOptions(Iterable<String> options) {
      optionsBuilder.addAll(options);
      return this;
    }

    public NewVmConfig build() {
      return new NewVmConfig(this);
    }
  }
}
