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

package com.google.caliper.util;

import static org.junit.Assert.assertEquals;

import com.google.gson.Gson;
import com.google.gson.JsonElement;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;

@SuppressWarnings("resource")
@RunWith(JUnit4.class)
public final class InterleavedReaderTest {

  @Test public void basic() throws IOException {
    InterleavedReader reader = new InterleavedReader("////", new StringReader("abc\n"
        + "////{\"z\":0}\n"
        + "////{\"y\":0}\n"
        + "def////{\"x\":0}\n"
        + "ghi\n"
        + "jkl\n"
        + "////{\"w\":0}"));

    assertEquals("abc\n", reader.read());
    assertEqualsJson("{\"z\":0}", reader.read());
    assertEqualsJson("{\"y\":0}", reader.read());
    assertEquals("def", reader.read());
    assertEqualsJson("{\"x\":0}", reader.read());
    assertEquals("ghi\njkl\n", reader.read());
    assertEqualsJson("{\"w\":0}", reader.read());
    assertEquals(null, reader.read());
  }

  @Test public void endsWithMarkerPrefix() throws IOException {
    InterleavedReader reader = new InterleavedReader("////", new StringReader("abc///"));
    assertEquals("abc", reader.read());
    assertEquals("///", reader.read());
    assertEquals(null, reader.read());
  }

  @Test public void newlineCharacters() throws IOException {
    InterleavedReader reader = new InterleavedReader("////", new StringReader("abc"
        + "////{\"z\":\"y\\nx\"}"));
    assertEquals("abc", reader.read());
    assertEqualsJson("{\"z\":\"y\\nx\"}", reader.read());
    assertEquals(null, reader.read());
  }

  /**
   * This test demonstrates that when the underlying stream only returns one
   * character at a time, the reader will read until the prefix is unambiguous.
   */
  @Test public void characterByCharacter() throws IOException {
    InterleavedReader reader = new InterleavedReader("////", charByCharReader("abc"
        + "////{\"z\":0}\n"
        + "///def///ghi/j/k////{\"y\":0}\n"
        + "///"));
    assertEquals("a", reader.read());
    assertEquals("b", reader.read());
    assertEquals("c", reader.read());
    assertEqualsJson("{\"z\":0}", reader.read());
    assertEquals("///d", reader.read());
    assertEquals("e", reader.read());
    assertEquals("f", reader.read());
    assertEquals("///g", reader.read());
    assertEquals("h", reader.read());
    assertEquals("i", reader.read());
    assertEquals("/j", reader.read());
    assertEquals("/k", reader.read());
    assertEqualsJson("{\"y\":0}", reader.read());
    assertEquals("///", reader.read());
  }

  @Test public void inputBufferedReader() throws IOException {
    InterleavedReader reader = new InterleavedReader("////",
        new BufferedReader(new StringReader("abc////{\"z\":0}\n"), 2));
    assertEquals("abc", reader.read());
    assertEqualsJson("{\"z\":0}", reader.read());
    assertEquals(null, reader.read());
  }

  private void assertEqualsJson(String expected, Object jsonObject) {
    assertEquals(expected, new Gson().toJson((JsonElement) jsonObject));
  }

  public Reader charByCharReader(final String s) {
    return new Reader() {
      StringReader delegate = new StringReader(s);
      @Override public int read(char[] buffer, int offset, int length) throws IOException {
        return delegate.read(buffer, offset, 1);
      }
      @Override public void close() {}
    };
  }
}
