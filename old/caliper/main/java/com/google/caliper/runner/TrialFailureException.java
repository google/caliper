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

package com.google.caliper.runner;

/**
 * An exception representing the failure of an individual trial. Throwing this exception will
 * invalidate the trial, but allow the run to continue. Both the runner and individual instruments
 * are free to throw this exception.
 *
 * <p>The exception message is used to convey the nature of the failure to the user.
 */
final class TrialFailureException extends RuntimeException {
  public TrialFailureException(String message) {
    super(message);
  }
}
