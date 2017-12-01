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

package com.google.caliper.runner;

import com.google.auto.value.AutoValue;
import com.google.caliper.runner.config.VmConfig;
import com.google.caliper.runner.platform.Platform;

/** A target VM for running benchmarks. */
@AutoValue
abstract class Target {

  // In the future this will also include device.

  /** Creates a new {@link Target}. */
  public static Target create(String name, VmConfig vm) {
    return new AutoValue_Target(name, vm);
  }

  /** Returns the name of this target. */
  public abstract String name();

  // Note: Platform is *currently* not actually target specific, but rather global to the VM
  // the runner is running on. However, this serves as a decent proxy for device until that
  // abstraction exists and lets us change code to get platform-specific things from the target
  // itself.

  /** Returns the platform this target is on. */
  public final Platform platform() {
    return vm().platform();
  }

  /** Returns the VM configuration for this target. */
  public abstract VmConfig vm();
}
