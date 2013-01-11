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

import com.google.common.base.Optional;

/**
 * A message representing generic, miscellaneous output.
 */
public final class GenericLogMessage extends LogMessage {
  public static final class Parser implements TryParser<GenericLogMessage> {
    @Override
    public Optional<GenericLogMessage> tryParse(String text) {
      return Optional.of(new GenericLogMessage());
    }
  }

  private GenericLogMessage() {}

  @Override
  public void accept(LogMessageVisitor visitor) {
    visitor.visit(this);
  }
}
