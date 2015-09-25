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

package dk.ilios.caliperx.config;

import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Objects;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;

import java.io.File;

import dk.ilios.caliperx.model.VmSpec;

/**
 * This is the configuration passed to the VM by the user. This differs from the {@link VmSpec}
 * in that any number of configurations can yield the same spec (due to default flag values) and any
 * number of specs can come from a single configuration (due to
 * <a href="http://www.oracle.com/technetwork/java/ergo5-140223.html">ergonomics</a>).
 *
 * @author gak@google.com (Gregory Kick)
 */
public final class VmConfig {
  private final File javaHome;
  private final ImmutableList<String> options;

//  @GuardedBy("this")
  private File javaExecutable;

  private VmConfig(Builder builder) {
    this.javaHome = builder.javaHome;
    this.options = builder.optionsBuilder.build();
  }

  @VisibleForTesting public VmConfig(File javaHome, Iterable<String> options, File javaExecutable) {
    this.javaHome = checkNotNull(javaHome);
    this.javaExecutable = checkNotNull(javaExecutable);
    this.options = ImmutableList.copyOf(options);
  }

  public File javaHome() {
    return javaHome;
  }

  public synchronized File javaExecutable() {
    // TODO(gak): move this logic somewhere else so that the IO (file stats) aren't performed here.
    if (javaExecutable == null) {
      // TODO(gak): support other platforms. This currently supports finding the java executable on
      // standard configurations of unix systems and windows.
      File bin = new File(javaHome, "bin");
      Preconditions.checkState(bin.exists() && bin.isDirectory(), 
          "Could not find %s under java home %s", bin, javaHome);
      File jvm = new File(bin, "java");
      if (!jvm.exists() || jvm.isDirectory()) {
        jvm = new File(bin, "java.exe");
        if (!jvm.exists() || jvm.isDirectory()) {
          throw new IllegalStateException(
              String.format("Cannot find java binary in %s, looked for java and java.exe", bin));
        }
      }
      javaExecutable = jvm;
    }
    return javaExecutable;
  }

  public ImmutableList<String> options() {
    return options;
  }

  @Override public boolean equals(Object obj) {
    if (obj == this) {
      return true;
    } else if (obj instanceof VmConfig) {
      VmConfig that = (VmConfig) obj;
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

  @VisibleForTesting public static final class Builder {
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

    public VmConfig build() {
      return new VmConfig(this);
    }
  }
}
