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


/**
 * A message representing output produced by the JVM when {@code -XX:+PrintFlagsFinal} is enabled.
 */
public final class VmOptionLogMessage extends LogMessage {
  private final String name;
  private final String value;

  VmOptionLogMessage(String name, String value) {
    this.name = checkNotNull(name);
    this.value = checkNotNull(value);
  }

  public String name() {
    return name;
  }

  public String value() {
    return value;
  }

  @Override
  public void accept(LogMessageVisitor visitor) {
    visitor.visit(this);
  }
}
