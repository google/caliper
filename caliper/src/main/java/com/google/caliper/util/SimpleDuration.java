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

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.util.EnumSet;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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

    BigDecimal nanosForUnit = UNIT_TO_NANOS.get(ABBREV_TO_UNIT.get(abbrev));
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

  public SimpleDuration plus(SimpleDuration that) {
    return new SimpleDuration(nanos + that.nanos);
  }

  public SimpleDuration times(int that) {
    return new SimpleDuration(nanos * that);
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
    TimeUnit bestUnit = NANOSECONDS;
    for (TimeUnit unit : EnumSet.complementOf(EnumSet.of(NANOSECONDS))) {
      if (unit.toNanos(1) > nanos) {
        break;
      }
      bestUnit = unit;
    }
    BigDecimal divisor = UNIT_TO_NANOS.get(bestUnit);

    BigDecimal d = new BigDecimal(nanos).divide(divisor, 2, RoundingMode.HALF_EVEN);
    return d + ABBREVIATIONS.get(bestUnit).get(0);
  }

  private static final ImmutableListMultimap<TimeUnit, String> ABBREVIATIONS = createAbbreviations();

  private static ImmutableListMultimap<TimeUnit, String> createAbbreviations() {
    ImmutableListMultimap.Builder<TimeUnit, String> builder = ImmutableListMultimap.builder();
    builder.putAll(TimeUnit.NANOSECONDS, "ns", "nanos");
    builder.putAll(TimeUnit.MICROSECONDS, "\u03bcs" /*μs*/, "us", "micros");
    builder.putAll(TimeUnit.MILLISECONDS, "ms", "millis");
    builder.putAll(TimeUnit.SECONDS, "s", "sec");

    // Do the rest in a JDK5-safe way
    TimeUnit[] allUnits = TimeUnit.values();
    if (allUnits.length >= 7) {
      builder.putAll(allUnits[4], "m", "min");
      builder.putAll(allUnits[5], "h", "hr");
      builder.putAll(allUnits[6], "d");
    }

    for (TimeUnit unit : TimeUnit.values()) {
      builder.put(unit, Ascii.toLowerCase(unit.name()));
    }
    return builder.build();
  }

  private static final Map<String, TimeUnit> ABBREV_TO_UNIT = createAbbrevToUnitMap();

  private static Map<String, TimeUnit> createAbbrevToUnitMap() {
    ImmutableMap.Builder<String, TimeUnit> builder = ImmutableMap.builder();
    for (Map.Entry<TimeUnit, String> entry : ABBREVIATIONS.entries()) {
      builder.put(entry.getValue(), entry.getKey());
    }
    return builder.build();
  }

  private static final Map<TimeUnit, BigDecimal> UNIT_TO_NANOS = createUnitToNanosMap();

  private static Map<TimeUnit, BigDecimal> createUnitToNanosMap() {
    ImmutableMap.Builder<TimeUnit, BigDecimal> builder = ImmutableMap.builder();
    for (TimeUnit unit : TimeUnit.values()) {
      builder.put(unit, new BigDecimal(unit.toNanos(1)));
    }
    return builder.build();
  }
}
