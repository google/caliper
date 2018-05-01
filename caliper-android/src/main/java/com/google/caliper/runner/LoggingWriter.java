/*
 * Copyright (C) 2017 Google Inc.
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

import android.util.Log;
import java.io.IOException;
import java.io.Writer;

/**
 * A {@code Writer} implementation that logs to an Android log.
 *
 * <p>This class is used as a replacement for the stdout and stderr streams that Caliper prints
 * messages it wants to appear to the user to. On Android, writes to {@code System.out} or {@code
 * System.err} just get discarded by default. This causes each line of text written to be logged
 * using Android's {@link Log} class with a specific tag and priority. The logged messages can then
 * be read using {@code adb logcat}.
 */
final class LoggingWriter extends Writer {
  private final LineBuffer lineBuffer;

  LoggingWriter(final int priority, final String tag, final String runId) {
    this.lineBuffer =
        new LineBuffer() {
          @Override
          protected void handleLine(String line, String end) {
            Log.println(priority, tag, "<" + runId + "> " + line);
          }
        };
  }

  @Override
  public void write(char[] cbuf, int off, int len) {
    try {
      lineBuffer.add(cbuf, off, len);
    } catch (IOException impossible) {
      throw new AssertionError(impossible);
    }
  }

  @Override
  public void write(String str, int off, int len) {
    append((CharSequence) str, off, off + len);
  }

  @Override
  public LoggingWriter append(CharSequence seq, int start, int end) {
    int len = end - start;
    char[] chars = new char[len];
    for (int i = 0; i < len; i++) {
      chars[i] = seq.charAt(start + i);
    }
    write(chars, 0, len);
    return this;
  }

  @Override
  public void flush() {}

  @Override
  public void close() {
    try {
      lineBuffer.finish();
    } catch (IOException impossible) {
      throw new AssertionError(impossible);
    }
  }
}
