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

/** Marker interface for classes the runner sends to the worker to tell it what to do. */
public interface WorkerRequest extends Serializable {
  /** Returns the type of the request. */
  // this is slightly weird, but with AutoValue, getClass() doesn't return quite what we want
  Class<? extends WorkerRequest> type();
}
