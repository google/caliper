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

import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;
import com.google.gson.Gson;
import com.google.inject.Inject;

/**
 * A message containing information on a failure encountered by the worker JVM.
 */
public class FailureLogMessage extends CaliperControlLogMessage {
  private static final String MESSAGE_PREFIX = CONTROL_PREFIX + "failed//";

  public static final class Parser
      implements TryParser<FailureLogMessage>, Renderer<FailureLogMessage> {
    private final Gson gson;

    @Inject
    Parser(Gson gson) {
      this.gson = gson;
    }

    @Override
    public Optional<FailureLogMessage> tryParse(String text) {
      return text.startsWith(MESSAGE_PREFIX)
          ? Optional.of(gson.fromJson(text.substring(MESSAGE_PREFIX.length()),
              FailureLogMessage.class))
          : Optional.<FailureLogMessage>absent();
    }

    @Override
    public String render(FailureLogMessage message) {
      return MESSAGE_PREFIX + gson.toJson(message);
    }
  }

  private final String exceptionClassName;
  private final String message;
  private final ImmutableList<StackTraceElement> stackTrace;

  public FailureLogMessage(String exceptionClassName, String message,
      ImmutableList<StackTraceElement> stackTrace) {
    this.exceptionClassName = checkNotNull(exceptionClassName);
    this.message = checkNotNull(message);
    this.stackTrace = checkNotNull(stackTrace);
  }

  public String exceptionClassName() {
    return exceptionClassName;
  }

  public String message() {
    return message;
  }

  public ImmutableList<StackTraceElement> stackTrace() {
    return stackTrace;
  }

  @Override
  public void accept(LogMessageVisitor visitor) {
    visitor.visit(this);
  }
}
