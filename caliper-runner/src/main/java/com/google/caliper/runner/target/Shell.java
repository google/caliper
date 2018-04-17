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

package com.google.caliper.runner.target;

import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.base.Joiner;
import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.common.io.CharStreams;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import javax.inject.Inject;

/** Simple helper for running shell commands. */
final class Shell {

  private static final Joiner COMMAND_JOINER = Joiner.on(' ');
  private static final Splitter COMMAND_SPLITTER = Splitter.on(' ');

  private final ExecutorService executor;
  private final ImmutableMap<String, String> env;

  @Inject
  Shell(ExecutorService executor) {
    this(executor, ImmutableMap.of());
  }

  Shell(ExecutorService executor, Map<String, String> env) {
    this.executor = checkNotNull(executor);
    this.env = ImmutableMap.copyOf(env);
  }

  /** Returns a new shell that is equivalent to this one but with the given {@code env}. */
  public Shell withEnv(Map<String, String> env) {
    return new Shell(executor, env);
  }

  /** Executes the given {@code command}, splitting it into a series of arguments on spaces. */
  public Result execute(String command) {
    return execute(COMMAND_SPLITTER.splitToList(command));
  }

  /** Executes the command represented by the given series of arguments. */
  public Result execute(String first, String second, String... rest) {
    return execute(Lists.asList(first, second, rest));
  }

  /** Executes the given command. */
  public Result execute(List<String> command) {
    ProcessBuilder builder = new ProcessBuilder();
    builder.environment().putAll(env);
    builder.command(command);

    try {
      Process process = builder.start();
      Future<String> stdoutFuture = inputStreamToString(process.getInputStream());
      Future<String> stderrFuture = inputStreamToString(process.getErrorStream());
      int exitCode = process.waitFor();
      return new Result(command, exitCode, stdoutFuture.get(), stderrFuture.get());
    } catch (Exception e) {
      throw new ShellException(e);
    }
  }

  private Future<String> inputStreamToString(final InputStream in) {
    return executor.submit(
        new Callable<String>() {
          @Override
          public String call() throws IOException {
            // TODO(cgdecker): Figure out how to use the right charset for this.
            // In the case of adb, the stdout/stderr in question here are from the adb process
            // running on this (the runner) device. But of course there could be output from adb
            // shell, which is output from shell commands run on the Android device. I don't know
            // how adb deals with any discrepancy there in the first place. Beyond that, the default
            // charset of the JVM we're running on could have been set to something different than
            // than the default charset the adb command is using. I don't know how we can get a
            // "correct" charset for either situation.
            return CharStreams.toString(new InputStreamReader(in, Charset.defaultCharset()));
          }
        });
  }

  /** The result of executing a shell command. */
  public static final class Result {
    private final ImmutableList<String> command;
    private final int exitCode;
    private final String stdout;
    private final String stderr;

    Result(List<String> command, int exitCode, String stdout, String stderr) {
      this.command = ImmutableList.copyOf(command);
      this.exitCode = exitCode;
      this.stdout = stdout.trim();
      this.stderr = stderr.trim();
    }

    /** Returns the command that was executed. */
    public ImmutableList<String> command() {
      return command;
    }

    /** Returns whether or not the command was successful, based on its exit code. */
    public boolean isSuccessful() {
      return exitCode == 0;
    }

    /** Returns the exit code of the adb command. */
    public int exitCode() {
      return exitCode;
    }

    /** Returns the full stdout from the command. */
    public String stdout() {
      return stdout;
    }

    /** Returns the full stderr from the command. */
    public String stderr() {
      return stderr;
    }

    /**
     * If this result is not successful, throws an exception using a default message giving the
     * command that failed and including the stderr output. Otherwise, returns this result.
     */
    public Result orThrow() {
      return orThrow("command failed: " + COMMAND_JOINER.join(command));
    }

    /**
     * If this result is not successful, throws an exception using the given message (and including
     * the stderr output). Otherwise, returns this result.
     */
    public Result orThrow(String message) {
      if (!isSuccessful()) {
        throw new ShellException(message, this);
      }
      return this;
    }
  }
}
