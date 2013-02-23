/*
 * Copyright (C) 2013 Google Inc.
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

import static com.google.caliper.bridge.CaliperControlLogMessage.CONTROL_PREFIX;

import com.google.gson.Gson;
import com.google.inject.Inject;

/**
 * Renders {@link CaliperControlLogMessage} instances to a format suitable for communication between
 * the runner and worker.
 */
final class ControlLogMessageRenderer implements Renderer<CaliperControlLogMessage> {
  private final Gson gson;

  @Inject ControlLogMessageRenderer(Gson gson) {
    this.gson = gson;
  }

  @Override public String render(CaliperControlLogMessage controlLogMessage) {
    StringBuilder builder = new StringBuilder(CONTROL_PREFIX)
        .append(controlLogMessage.getClass().getSimpleName())
        .append("//");
    gson.toJson(controlLogMessage, builder);
    return builder.toString();
  }
}
