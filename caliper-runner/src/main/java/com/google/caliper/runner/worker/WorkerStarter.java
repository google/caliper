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

package com.google.caliper.runner.worker;

import java.util.UUID;

/**
 * An object that starts {@linkplain WorkerProcess worker processes} by running
 * {@linkplain Command commands}.
 *
 * @author Colin Decker
 */
interface WorkerStarter {

  // This will likely become the device abstraction, so the name isn't likely to stick around.

  /**
   * Starts the worker process for the given {@code id} by running the given {@code command}.
   */
  WorkerProcess startWorker(UUID id, Command command) throws Exception;
}
