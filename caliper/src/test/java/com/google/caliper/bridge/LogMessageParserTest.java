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

package com.google.caliper.bridge;

import static com.google.caliper.bridge.GcLogMessage.Type.FULL;
import static com.google.caliper.bridge.GcLogMessage.Type.INCREMENTAL;
import static com.google.common.base.Charsets.UTF_8;
import static java.util.concurrent.TimeUnit.MICROSECONDS;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import com.google.caliper.util.ShortDuration;
import com.google.common.io.Resources;

import dagger.Component;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.util.List;

import javax.inject.Inject;

/**
 * Tests {@link LogMessageParser}.
 */
@RunWith(JUnit4.class)

public class LogMessageParserTest {
  @Inject LogMessageParser parser;

  @Component(modules = BridgeModule.class)
  interface LogMessageParserComponent {
    void inject(LogMessageParserTest test);
  }

  @Before public void setUp() {
    DaggerLogMessageParserTest_LogMessageParserComponent.create().inject(this);
  }

  @Test public void gcPatten_jdk6() throws Exception {
    List<String> lines = Resources.readLines(
        Resources.getResource(LogMessageParserTest.class, "jdk6-gc.txt"), UTF_8);
    for (String line : lines) {
      assertTrue(parser.parse(line) instanceof GcLogMessage);
    }
  }

  @Test public void gcPatten_jdk7() throws Exception {
    List<String> lines = Resources.readLines(
        Resources.getResource(LogMessageParserTest.class, "jdk7-gc.txt"), UTF_8);
    for (String line : lines) {
      assertTrue(parser.parse(line) instanceof GcLogMessage);
    }
  }

  @Test public void gcMessageData() {
    assertEquals(new GcLogMessage(INCREMENTAL, ShortDuration.of(1232, MICROSECONDS)),
        parser.parse("[GC 987K->384K(62848K), 0.0012320 secs]"));
    assertEquals(new GcLogMessage(FULL, ShortDuration.of(5455, MICROSECONDS)),
        parser.parse("[Full GC 384K->288K(62848K), 0.0054550 secs]"));
    assertEquals(new GcLogMessage(INCREMENTAL, ShortDuration.of(1424, MICROSECONDS)),
        parser.parse(
            "2013-02-11T20:15:26.706-0600: 0.098: [GC 1316K->576K(62848K), 0.0014240 secs]"));
    assertEquals(new GcLogMessage(FULL, ShortDuration.of(4486, MICROSECONDS)),
        parser.parse(
            "2013-02-11T20:15:26.708-0600: 0.099: [Full GC 576K->486K(62848K), 0.0044860 secs]"));
  }

  @Test public void jitPattern_jdk6() throws Exception {
    List<String> lines = Resources.readLines(
        Resources.getResource(LogMessageParserTest.class, "jdk6-compilation.txt"), UTF_8);
    for (String line : lines) {
      assertTrue(parser.parse(line) instanceof HotspotLogMessage);
    }
  }

  @Test public void jitPattern_jdk7() throws Exception {
    List<String> lines = Resources.readLines(
        Resources.getResource(LogMessageParserTest.class, "jdk7-compilation.txt"), UTF_8);
    for (String line : lines) {
      assertTrue(parser.parse(line) instanceof HotspotLogMessage);
    }
  }

  @Test public void vmOptionPattern_jdk6() throws Exception {
    List<String> lines = Resources.readLines(
        Resources.getResource(LogMessageParserTest.class, "jdk6-flags.txt"), UTF_8);
    for (String line : lines) {
      assertTrue(parser.parse(line) instanceof VmOptionLogMessage);
    }
  }

  @Test public void vmOptionPattern_jdk7() throws Exception {
    List<String> lines = Resources.readLines(
        Resources.getResource(LogMessageParserTest.class, "jdk7-flags.txt"), UTF_8);
    for (String line : lines) {
      assertTrue(parser.parse(line) instanceof VmOptionLogMessage);
    }
  }
}
