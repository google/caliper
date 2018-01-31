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

import com.google.auto.value.AutoValue;
import com.google.common.base.Throwables;
import java.io.Serializable;
import javax.annotation.Nullable;

/** A message containing information on a failure encountered by the worker JVM. */
@AutoValue
public abstract class FailureLogMessage extends LogMessage implements Serializable {
  private static final long serialVersionUID = 1L;

  /** Creates a new log message for the given {@code Throwable}. */
  public static FailureLogMessage create(Throwable e) {
    return new AutoValue_FailureLogMessage(
        e.getClass().getName(), e.getMessage(), Throwables.getStackTraceAsString(e));
  }

  /** The name of the type of the exception that was thrown. */
  public abstract String exceptionType();

  /** Returns the message of the exception that was thrown. */
  @Nullable
  public abstract String message();

  /** The full stack trace of the exception. */
  public abstract String stackTrace();

  @Override
  public final void accept(LogMessageVisitor visitor) {
    visitor.visit(this);
  }
}
