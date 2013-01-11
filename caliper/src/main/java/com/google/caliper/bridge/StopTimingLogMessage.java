/*
 * Copyright (C) 2012 Google Inc.
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

import com.google.caliper.model.Measurement;
import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;
import com.google.gson.Gson;
import com.google.inject.Inject;

/**
 * A message signaling that the timing interval has ended in the worker.
 */
// TODO(gak): rename in terms of measurement
public class StopTimingLogMessage extends CaliperControlLogMessage {
  private static final String MESSAGE_PREFIX = CONTROL_PREFIX + "measurement//";

  public static final class Parser implements TryParser<StopTimingLogMessage>,
      Renderer<StopTimingLogMessage> {
    private final Gson gson;

    @Inject Parser(Gson gson) {
      this.gson = gson;
    }

    @Override public Optional<StopTimingLogMessage> tryParse(String text) {
      return text.startsWith(MESSAGE_PREFIX)
          ? Optional.of(
              gson.fromJson(text.substring(MESSAGE_PREFIX.length()), StopTimingLogMessage.class))
          : Optional.<StopTimingLogMessage>absent();
    }

    @Override public String render(StopTimingLogMessage message) {
      return MESSAGE_PREFIX + gson.toJson(message);
    }
  }

  private final ImmutableList<Measurement> measurements;

  public StopTimingLogMessage(Iterable<Measurement> measurements) {
    this.measurements = ImmutableList.copyOf(measurements);
  }

  public ImmutableList<Measurement> measurements() {
    return measurements;
  }

  @Override public void accept(LogMessageVisitor visitor) {
    visitor.visit(this);
  }
}
