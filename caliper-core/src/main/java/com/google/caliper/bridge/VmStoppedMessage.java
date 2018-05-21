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
import java.io.Serializable;
import java.util.UUID;

/** A message sent from a device to the runner to tell it that a VM stopped. */
@AutoValue
public abstract class VmStoppedMessage implements Serializable {
  private static final long serialVersionUID = 1L;

  VmStoppedMessage() {}

  /** Creates a new {@link VmStoppedMessage}. */
  public static VmStoppedMessage create(UUID vmId, int exitCode) {
    return new AutoValue_VmStoppedMessage(vmId, exitCode);
  }

  /** Returns the UUID identifying the VM that stopped. */
  public abstract UUID vmId();

  /** Returns the exit code of the process. */
  public abstract int exitCode();
}
