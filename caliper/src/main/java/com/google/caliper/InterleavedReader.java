/*
 * Copyright (C) 2010 Google Inc.
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

package com.google.caliper;

import com.google.gson.JsonParser;
import java.io.BufferedReader;
import java.io.Closeable;
import java.io.IOException;
import java.io.Reader;

/**
 * Reads a stream containing inline JSON objects. Each JSON object is prefixed
 * by a marker string and suffixed by a newline character.
 */
public final class InterleavedReader implements Closeable {

  /**
   * The length of the scratch buffer to search for markers in. Also acts as an
   * upper bound on the length of returned strings. Not used as an I/O buffer.
   */
  private static final int BUFFER_LENGTH = 80;

  private final String marker;
  private final BufferedReader reader;
  private final JsonParser jsonParser = new JsonParser();

  public InterleavedReader(String marker, Reader reader) {
    if (marker.length() > BUFFER_LENGTH) {
      throw new IllegalArgumentException("marker.length() > BUFFER_LENGTH");
    }
    this.marker = marker;
    this.reader = reader instanceof BufferedReader
        ? (BufferedReader) reader
        : new BufferedReader(reader);
  }

  /**
   * Returns the next value in the stream: either a String, a JsonElement, or
   * null to indicate the end of the stream. Callers should use instanceof to
   * inspect the return type.
   */
  public Object read() throws IOException {
    char[] buffer = new char[BUFFER_LENGTH];
    reader.mark(BUFFER_LENGTH);
    int count = 0;
    int textEnd;

    while (true) {
      int r = reader.read(buffer, count, buffer.length - count);

      if (r == -1) {
        // the input is exhausted; return the remaining characters
        textEnd = count;
        break;
      }

      count += r;
      int possibleMarker = findPossibleMarker(buffer, count);

      if (possibleMarker != 0) {
        // return the characters that precede the marker
        textEnd = possibleMarker;
        break;
      }

      if (count < marker.length()) {
        // the buffer contains only the prefix of a marker so we must read more
        continue;
      }

      // we've read a marker so return the value that follows
      reader.reset();
      String json = reader.readLine().substring(marker.length());
      return jsonParser.parse(json);
    }

    if (count == 0) {
      return null;
    }

    // return characters
    reader.reset();
    count = reader.read(buffer, 0, textEnd);
    return new String(buffer, 0, count);
  }

  @Override public void close() throws IOException {
    reader.close();
  }

  /**
   * Returns the index of marker in {@code chars}, stopping at {@code limit}.
   * Should the chars end with a prefix of marker, the offset of that prefix
   * is returned.
   */
  int findPossibleMarker(char[] chars, int limit) {
    search:
    for (int i = 0; true; i++) {
      for (int m = 0; m < marker.length() && i + m < limit; m++) {
        if (chars[i + m] != marker.charAt(m)) {
          continue search;
        }
      }
      return i;
    }
  }
}
