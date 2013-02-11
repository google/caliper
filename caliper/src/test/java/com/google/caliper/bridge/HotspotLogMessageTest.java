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

import static com.google.common.base.Charsets.UTF_8;
import static org.junit.Assert.assertTrue;

import com.google.common.io.Resources;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.io.IOException;
import java.util.List;

/**
 * Tests {@link HotspotLogMessage}.
 */

@RunWith(JUnit4.class)
public class HotspotLogMessageTest {
  @Test public void pattern_jdk6() throws IOException {
    List<String> flagLines = Resources.readLines(
        Resources.getResource(HotspotLogMessageTest.class, "jdk6-compilation.txt"), UTF_8);
    for (String flagLine : flagLines) {
      assertTrue(flagLine, HotspotLogMessage.PATTERN.matcher(flagLine).matches());
    }
  }

  @Test public void pattern_jdk7() throws IOException {
    List<String> flagLines = Resources.readLines(
        Resources.getResource(HotspotLogMessageTest.class, "jdk7-compilation.txt"), UTF_8);
    for (String flagLine : flagLines) {
      assertTrue(flagLine, HotspotLogMessage.PATTERN.matcher(flagLine).matches());
    }
  }
}
