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

/** A request that's sent to a device to request that it kill a VM process. */
@AutoValue
public abstract class KillVmRequest implements Serializable {

  KillVmRequest() {}

  /** Creates a new {@link KillVmRequest}. */
  public static KillVmRequest create(UUID vmId) {
    return new AutoValue_KillVmRequest(vmId);
  }

  /** Returns the UUID of the VM to kill. */
  public abstract UUID vmId();
}
