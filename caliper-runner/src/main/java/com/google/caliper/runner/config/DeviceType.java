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

package com.google.caliper.runner.config;

import com.google.common.base.Joiner;

/** Enumeration of types of devices supported by Caliper. */
public enum DeviceType {
  /** The device on which Caliper itself is running. */
  LOCAL,

  /** An Android device/emulator connected via ADB. */
  ADB;

  /** Gets the {@link DeviceType} for the given {@code type} field string. */
  public static DeviceType of(String type) {
    switch (type) {
      case "local":
        return LOCAL;
      case "adb":
        return ADB;
      default:
        throw new InvalidConfigurationException(
            String.format(
                "Invalid device type: %s (supported types are: %s)",
                type, Joiner.on(", ").join(values())));
    }
  }

  /** The name of this device type, matching how it should be specified in configuration. */
  final String name;

  DeviceType() {
    this.name = name().toLowerCase();
  }

  @Override
  public String toString() {
    return name;
  }
}
