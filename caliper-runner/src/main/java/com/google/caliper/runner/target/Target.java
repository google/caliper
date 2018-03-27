/*
 * Copyright (C) 2011 Google Inc.
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

import com.google.auto.value.AutoValue;

/** A specific VM on a specific device on which benchmarks may be run. */
@AutoValue
public abstract class Target {

  /**
   * Creates a new target for the given {@code vm} on the given {@code device}. @ if the VM is
   * invalid or doesn't exist on the device.
   */
  static Target create(Device device, Vm vm) {
    return new AutoValue_Target(device, vm, device.vmExecutablePath(vm));
  }

  /** Returns a name for this target. */
  public final String name() {
    return vm().name() + '@' + device().name();
  }

  /** Returns the target device. */
  public abstract Device device();

  /** Returns the target VM. */
  public abstract Vm vm();

  /** Returns the absolute path to the VM executable on the target device. */
  public abstract String vmExecutablePath();
}
