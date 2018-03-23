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

package com.google.caliper.runner.target;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableList;
import java.io.File;
import java.io.InputStream;
import javax.inject.Inject;

/**
 * {@link Device} for the local machine.
 *
 * @author Colin Decker
 */
public final class LocalDevice extends Device {

  private static final Joiner ARG_JOINER = Joiner.on(' ');

  private final boolean redirectErrorStream;

  @Inject
  public LocalDevice(ShutdownHookRegistrar shutdownHookRegistrar) {
    this(shutdownHookRegistrar, false);
  }

  @VisibleForTesting
  public LocalDevice(
      ShutdownHookRegistrar shutdownHookRegistrar, boolean redirectErrorStream) {
    super(shutdownHookRegistrar);
    this.redirectErrorStream = redirectErrorStream;
  }

  @Override
  protected void startUp() {}

  @Override
  protected void shutDown() {}

  @Override
  public VmProcess doStartVm(VmProcess.Spec spec, VmProcess.Logger logger) throws Exception {
    ProcessBuilder builder = new ProcessBuilder().redirectErrorStream(redirectErrorStream);
    builder.environment().putAll(spec.vm().platform().workerEnvironment());

    ImmutableList<String> command = createCommand(spec);
    logger.log("Command: " + ARG_JOINER.join(command) + "\n");
    builder.command(command);

    return new LocalProcess(builder.start());
  }

  @VisibleForTesting
  ImmutableList<String> createCommand(VmProcess.Spec spec) {
    return new ImmutableList.Builder<String>()
        .add(spec.vm().platform().vmExecutable(new File(spec.vm().home().get())).getAbsolutePath())
        .addAll(spec.vmOptions())
        .add(spec.mainClass())
        .addAll(spec.mainArgs())
        .build();
  }

  /** A worker process running on the local machine. */
  private static final class LocalProcess extends VmProcess {

    private final Process process;

    LocalProcess(Process process) {
      this.process = process;
    }

    @Override
    public InputStream stdout() {
      return process.getInputStream();
    }

    @Override
    public InputStream stderr() {
      return process.getErrorStream();
    }

    @Override
    public int doAwaitExit() throws InterruptedException {
      return process.waitFor();
    }

    @Override
    public void doKill() {
      process.destroy();
    }
  }
}
