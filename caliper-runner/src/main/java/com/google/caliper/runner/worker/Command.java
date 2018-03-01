/*
 * Copyright (C) 2017 Google Inc.
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

package com.google.caliper.runner.worker;

import com.google.auto.value.AutoValue;
import com.google.common.base.Joiner;
import com.google.common.base.MoreObjects;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import java.util.Map;

/**
 * A simple representation of a command that can be run to start a process on a device.
 *
 * @author Colin Decker
 */
@AutoValue
public abstract class Command {

  /**
   * Returns a new builder for creating {@link Command} objects.
   */
  public static Builder builder() {
    return new AutoValue_Command.Builder();
  }

  /**
   * Returns a map of key-value pairs that should be added to the environment when executing this
   * command.
   */
  public abstract ImmutableMap<String, String> environment();

  /**
   * Returns the full list of arguments that make up this command, including the binary to run.
   */
  public abstract ImmutableList<String> arguments();

  private static final Joiner SPACE_JOINER = Joiner.on(' ');

  /**
   * Returns the full list of arguments that make up this command, including the binary to run, as a
   * single string with a space between each argument.
   */
  public final String argumentsString() {
    return SPACE_JOINER.join(arguments());
  }

  @Override
  public String toString() {
    return MoreObjects.toStringHelper(Command.class.getSimpleName())
        .add("environment", environment())
        .add("arguments", argumentsString())
        .toString();
  }

  /**
   * Builder for creating {@link Command} objects.
   */
  @AutoValue.Builder
  public static abstract class Builder {

    /** Adds the given argument to the command. */
    public final Builder addArgument(Object arg) {
      argumentsBuilder().add(arg.toString());
      return this;
    }

    /** Adds the given arguments to the command. */
    public final Builder addArguments(Iterable<?> args) {
      for (Object arg : args) {
        argumentsBuilder().add(arg.toString());
      }
      return this;
    }

    /**
     * Returns a builder that can be used for adding arguments to the command.
     */
    abstract ImmutableList.Builder<String> argumentsBuilder();

    /**
     * Puts the given environment variable into the environment for the command.
     */
    public final Builder putEnvironmentVariable(String key, String value) {
      environmentBuilder().put(key, value);
      return this;
    }

    /**
     * Puts the given variables into the environment for the command.
     */
    public final Builder putAllEnvironmentVariables(Map<String, String> variables) {
      environmentBuilder().putAll(variables);
      return this;
    }

    /**
     * Returns a builder that can be used for adding entries to the environment for the command.
     */
    abstract ImmutableMap.Builder<String, String> environmentBuilder();

    /**
     * Builds a new {@link Command}.
     */
    abstract Command build();
  }
}
