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

package com.google.caliper.bridge;

import com.google.auto.value.AutoValue;
import com.google.common.collect.ImmutableList;
import java.io.Serializable;
import java.util.UUID;

/**
 * A request to send to a process running on a device to tell it to run a given command to start a
 * VM.
 */
@AutoValue
public abstract class StartVmRequest implements Serializable {

  StartVmRequest() {}

  /** Creates a new {@link StartVmRequest}. */
  public static StartVmRequest create(UUID vmId, Iterable<String> command) {
    return new AutoValue_StartVmRequest(
        vmId, ImmutableList.copyOf(command), UUID.randomUUID(), UUID.randomUUID());
  }

  /** Returns the UUID that identifies the VM process. */
  public abstract UUID vmId();

  /** Returns the command to run. */
  public abstract ImmutableList<String> command();

  /** Returns the UUID that identifies the VM process's stdout stream. */
  public abstract UUID stdoutId();

  /** Returns the UUID that identifies the VM process's stderr stream. */
  public abstract UUID stderrId();
}
