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

import static java.util.concurrent.TimeUnit.SECONDS;

import com.google.caliper.util.Parser;
import com.google.caliper.util.ShortDuration;

import java.math.BigDecimal;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.inject.Inject;

/**
 * Parses {@link LogMessage} strings.
 */
final class LogMessageParser implements Parser<LogMessage> {
  @Inject LogMessageParser() {}

  private static final Pattern GC_PATTERN =
      Pattern.compile(".*\\[(?:(Full) )?GC.*(\\d+\\.\\d+) secs\\]");
  private static final Pattern JIT_PATTERN =
      Pattern.compile(".*::.*( \\(((\\d+ bytes)|(static))\\))?");
  private static final Pattern VM_OPTION_PATTERN =
      Pattern.compile("\\s*(\\w+)\\s+(\\w+)\\s+:?=\\s+([^\\s]*)\\s+\\{([^}]*)\\}\\s*");

  @Override public LogMessage parse(CharSequence text) {
    // TODO(gak): do this stuff in terms of CharSequence instead of String
    String string = text.toString();
    Matcher gcMatcher = GC_PATTERN.matcher(string);
    if (gcMatcher.matches()) {
      return new GcLogMessage(
          "Full".equals(gcMatcher.group(1))
              ? GcLogMessage.Type.FULL
              : GcLogMessage.Type.INCREMENTAL,
          ShortDuration.of(BigDecimal.valueOf(Double.parseDouble(gcMatcher.group(2))), SECONDS));
    }
    Matcher jitMatcher = JIT_PATTERN.matcher(string);
    if (jitMatcher.matches()) {
      return new HotspotLogMessage();
    }
    Matcher vmOptionMatcher = VM_OPTION_PATTERN.matcher(string);
    if (vmOptionMatcher.matches()) {
      return new VmOptionLogMessage(vmOptionMatcher.group(2), vmOptionMatcher.group(3));
    }
    return null;
  }
}
