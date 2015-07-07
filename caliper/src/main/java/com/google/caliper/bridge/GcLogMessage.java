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

import com.google.caliper.util.ShortDuration;
import com.google.common.base.MoreObjects;
import com.google.common.base.Objects;

/**
 * A message representing output produced by the JVM when {@code -XX:+PrintGC} is enabled.
 */
public final class GcLogMessage extends LogMessage {
  /**
   * The type of the garbage collection performed.
   */
  public static enum Type {
    FULL,
    INCREMENTAL,
  }

  private final Type type;
  private final ShortDuration duration;

  GcLogMessage(Type type, ShortDuration duration) {
    this.type = checkNotNull(type);
    this.duration = checkNotNull(duration);
  }

  public Type type() {
    return type;
  }

  public ShortDuration duration() {
    return duration;
  }

  @Override
  public void accept(LogMessageVisitor visitor) {
    visitor.visit(this);
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(type, duration);
  }

  @Override
  public boolean equals(Object obj) {
    if (obj == this) {
      return true;
    } else if (obj instanceof GcLogMessage) {
      GcLogMessage that = (GcLogMessage) obj;
      return this.type == that.type
          && this.duration.equals(that.duration);
    } else {
      return false;
    }
  }

  @Override
  public String toString() {
    return MoreObjects.toStringHelper(this)
        .addValue(type)
        .add("duration", duration)
        .toString();
  }
}
