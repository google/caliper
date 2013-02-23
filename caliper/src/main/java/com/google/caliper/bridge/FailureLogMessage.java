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

import com.google.common.base.Objects;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;

import java.util.Arrays;

/**
 * A message containing information on a failure encountered by the worker JVM.
 */
public class FailureLogMessage extends CaliperControlLogMessage {
  private final String exceptionClassName;
  private final String message;
  private final ImmutableList<StackTraceElement> stackTrace;

  public FailureLogMessage(Exception e) {
    this(e.getClass().getName(), Strings.nullToEmpty(e.getMessage()),
        Arrays.asList(e.getStackTrace()));
  }

  public FailureLogMessage(String exceptionClassName, String message,
      Iterable<StackTraceElement> stackTrace) {
    this.exceptionClassName = checkNotNull(exceptionClassName);
    this.message = checkNotNull(message);
    this.stackTrace = ImmutableList.copyOf(stackTrace);
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

  @Override
  public int hashCode() {
    return Objects.hashCode(exceptionClassName, message, stackTrace);
  }

  @Override
  public boolean equals(Object obj) {
    if (obj == this) {
      return true;
    } else if (obj instanceof FailureLogMessage) {
      FailureLogMessage that = (FailureLogMessage) obj;
      return this.exceptionClassName.equals(that.exceptionClassName)
          && this.message.equals(that.message)
          && this.stackTrace.equals(that.stackTrace);
    } else {
      return false;
    }
  }
}
