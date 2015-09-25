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

import com.google.common.base.Throwables;

import java.io.Serializable;

/**
 * A message containing information on a failure encountered by the worker JVM.
 */
public class FailureLogMessage extends LogMessage implements Serializable {
  private static final long serialVersionUID = 1L;

  private final String stackTrace;

  public FailureLogMessage(Throwable e) {
    this(Throwables.getStackTraceAsString(e));
  }

  public FailureLogMessage(String stackTrace) {
    this.stackTrace = checkNotNull(stackTrace);
  }

  public String stackTrace() {
    return stackTrace;
  }

  @Override
  public void accept(LogMessageVisitor visitor) {
    visitor.visit(this);
  }

  @Override
  public int hashCode() {
    return stackTrace.hashCode();
  }

  @Override
  public boolean equals(Object obj) {
    if (obj == this) {
      return true;
    } else if (obj instanceof FailureLogMessage) {
      FailureLogMessage that = (FailureLogMessage) obj;
      return this.stackTrace.equals(that.stackTrace);
    } else {
      return false;
    }
  }
}
