/*
 * Copyright (C) 2013 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */

package com.google.caliper.runner;

/**
 * A simple interface for registering and deregistering shutdown hooks.
 */
interface ShutdownHookRegistrar {
  /**
   * Adds a hook to run at process shutdown.
   * 
   * <p>See {@link Runtime#addShutdownHook(Thread)}.
   */
  void addShutdownHook(Thread hook);
  /**
   * Removes a shutdown hook that was previously registered via {@link #addShutdownHook(Thread)}.
   * 
   * <p>See {@link Runtime#removeShutdownHook(Thread)}.
   */
  boolean removeShutdownHook(Thread hook);
}

