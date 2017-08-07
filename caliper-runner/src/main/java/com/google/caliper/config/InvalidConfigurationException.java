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

package com.google.caliper.config;

import java.io.PrintWriter;

/**
 * Thrown when an invalid configuration has been specified by the user.
 *
 * @author gak@google.com (Gregory Kick)
 */
public final class InvalidConfigurationException extends RuntimeException {
  public InvalidConfigurationException() {
    super();
  }

  public InvalidConfigurationException(String message, Throwable cause) {
    super(message, cause);
  }

  public InvalidConfigurationException(String message) {
    super(message);
  }

  public InvalidConfigurationException(Throwable cause) {
    super(cause);
  }

  public void display(PrintWriter writer) {
    writer.println(getMessage());
  }
}
