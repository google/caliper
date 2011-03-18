/*
 * Copyright (C) 2011 Google Inc.
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

import static com.google.common.base.Preconditions.checkArgument;
import static java.util.concurrent.TimeUnit.NANOSECONDS;

import com.google.common.base.Ascii;
import com.google.common.collect.ImmutableListMultimap;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Multimap;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author Kevin Bourrillion
 */
public final class SimpleDuration {
  // fortunately no abbreviation starts with 'e', so this should work
  private static final Pattern PATTERN = Pattern.compile("^([0-9.eE+-]+) ?(\\S+)$");

  private static final MathContext ROUNDER = new MathContext(0, RoundingMode.UP);

  public static SimpleDuration of(long duration, TimeUnit unit) {
    return ofNanos(NANOSECONDS.convert(duration, unit));
  }

  public static SimpleDuration ofNanos(long nanos) {
    return new SimpleDuration(nanos);
  }

  public static SimpleDuration valueOf(String s) {
    Matcher matcher = PATTERN.matcher(s);
    checkArgument(matcher.matches(), "Invalid SimpleDuration: %s", s);

    BigDecimal value = new BigDecimal(matcher.group(1));
    String abbrev = matcher.group(2);

    BigDecimal nanosForUnit = ABBREV_TO_NANOS.get(abbrev);
    checkArgument(nanosForUnit != null, "Unrecognized time unit: %s", abbrev);

    BigDecimal nanos = value.multiply(nanosForUnit).setScale(0, RoundingMode.UP);
    return new SimpleDuration(nanos.longValueExact());
  }

  private final long nanos;

  public SimpleDuration(long nanos) {
    this.nanos = nanos;
  }

  public long toNanos() {
    return nanos;
  }

  @Override public boolean equals(Object object) {
    if (object instanceof SimpleDuration) {
      SimpleDuration that = (SimpleDuration) object;
      return this.nanos == that.nanos;
    }
    return false;
  }

  @Override public int hashCode() {
    return ((Long) nanos).hashCode();
  }

  @Override public String toString() {
    return nanos + "ns";
  }

  private static final Multimap<TimeUnit, String> ABBREVIATIONS = createAbbreviations();

  private static Multimap<TimeUnit, String> createAbbreviations() {
    ImmutableListMultimap.Builder<TimeUnit, String> builder = ImmutableListMultimap.builder();
    builder.putAll(NANOSECONDS, "ns", "nanos");
    builder.putAll(TimeUnit.MICROSECONDS, "us", "\u03bcs"); // μs
    builder.putAll(TimeUnit.MILLISECONDS, "ms", "millis");
    builder.putAll(TimeUnit.SECONDS, "s", "sec");

    // Uh oh, not JDK5-compatible...
    builder.putAll(TimeUnit.MINUTES, "m", "min");
    builder.putAll(TimeUnit.HOURS, "h", "hr");
    builder.putAll(TimeUnit.DAYS, "d");

    for (TimeUnit unit : TimeUnit.values()) {
      builder.put(unit, Ascii.toLowerCase(unit.name()));
    }
    return builder.build();
  }

  private static final Map<String, BigDecimal> ABBREV_TO_NANOS = createAbbrevToNanosMap();

  private static Map<String, BigDecimal> createAbbrevToNanosMap() {
    ImmutableMap.Builder<String, BigDecimal> builder = ImmutableMap.builder();
    for (Map.Entry<TimeUnit, String> entry : ABBREVIATIONS.entries()) {
      TimeUnit unit = entry.getKey();
      builder.put(entry.getValue(), new BigDecimal(unit.toNanos(1)));
    }
    return builder.build();
  }
}
