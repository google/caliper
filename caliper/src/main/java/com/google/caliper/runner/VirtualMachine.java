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

import com.google.caliper.config.VmConfig;
import com.google.common.base.MoreObjects;
import com.google.common.base.Objects;

/**
 * A named virtual machine configuration.
 */
final class VirtualMachine {
  final String name;
  final VmConfig config;

  VirtualMachine(String name, VmConfig config) {
    this.name = name;
    this.config = config;
  }

  @Override public boolean equals(Object object) {
    if (object instanceof VirtualMachine) {
      VirtualMachine that = (VirtualMachine) object;
      return this.name.equals(that.name)
          && this.config.equals(that.config);
    }
    return false;
  }

  @Override public int hashCode() {
    return Objects.hashCode(name, config);
  }

  @Override public String toString() {
    return MoreObjects.toStringHelper(this)
        .add("name", name)
        .add("config", config)
        .toString();
  }
}
