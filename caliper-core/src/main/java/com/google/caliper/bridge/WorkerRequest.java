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

package com.google.caliper.bridge;

import java.io.Serializable;
import java.util.UUID;

/**
 * Base class for classes the runner sends to the worker to tell it what to do.
 *
 * @author Colin Decker
 */
public abstract class WorkerRequest implements Serializable {

  private final UUID id;
  private final int port;

  protected WorkerRequest(UUID id, int port) {
    this.id = id;
    this.port = port;
  }

  /** Returns the ID of this worker. */
  public UUID id() {
    return id;
  }

  /**
   * Returns the port the worker should open a socket connection to for communication.
   */
  public int port() {
    return port;
  }
}
