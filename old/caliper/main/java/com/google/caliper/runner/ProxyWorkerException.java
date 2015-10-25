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

package com.google.caliper.runner;

import com.google.common.base.Joiner;
import com.google.common.base.Splitter;


/**
 * An exception created on the runner with the same stack trace as one thrown on the worker that
 * reports the actual exception class and message in its message.
 */
final class ProxyWorkerException extends RuntimeException {
  ProxyWorkerException(String stackTrace) {
    super(formatMesssage(stackTrace));
  }

  private static String formatMesssage(String stackTrace) {
    StringBuilder builder = new StringBuilder(stackTrace.length() + 512)
        .append("An exception occurred in a worker process.  The stack trace is as follows:\n\t");
    Joiner.on("\n\t").appendTo(builder, Splitter.on('\n').split(stackTrace));
    return builder.toString();
  }
}
