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

/** Message sent by a remote device to provide the local worker classpath on that device. */
@AutoValue
public abstract class RemoteClasspathMessage implements Serializable {
  private static final long serialVersionUID = 1L;

  RemoteClasspathMessage() {}

  // Note that if Caliper were at some point to use a runner proxy for something other than running
  // on Android devices, this message might need to instead be able to return a map of VM type to
  // the remote classpath for that VM type... but for now it just needs one classpath.

  /** Creates a new {@link RemoteClasspathMessage}. */
  public static RemoteClasspathMessage create(String classpath, String nativeLibraryDir) {
    return new AutoValue_RemoteClasspathMessage(classpath, nativeLibraryDir);
  }

  /** Returns the classpath. */
  public abstract String classpath();

  /** Returns the path to native libraries. */
  public abstract String nativeLibraryDir();
}
