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
import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.base.Ascii;
import com.google.common.collect.ImmutableListMultimap;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import com.google.common.primitives.Longs;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.annotation.Nullable;

/**
 * Represents a nonnegative duration from 0 to 100 days, with picosecond precision.
 * Contrast with Joda-Time's duration class, which has only millisecond precision but can
 * represent durations of millions of years.
 */
public abstract class ShortDuration implements Comparable<ShortDuration> {
  // Factories

  public static ShortDuration of(long duration, TimeUnit unit) {
    if (duration == 0) {
      return ZERO;
    }
    checkArgument(duration >= 0, "negative duration: %s", duration);
    checkArgument(duration <= MAXES.get(unit),
        "ShortDuration cannot exceed 100 days: %s %s", duration, unit);
    long nanos = TimeUnit.NANOSECONDS.convert(duration, unit);
    return new PositiveShortDuration(nanos * 1000);
  }

  public static ShortDuration of(BigDecimal duration, TimeUnit unit) {
    // convert to picoseconds first, to minimize rounding
    BigDecimal picos = duration.multiply(ONE_IN_PICOS.get(unit));
    return ofPicos(toLong(picos, RoundingMode.HALF_UP));
  }

  public static ShortDuration valueOf(String s) {
    if ("0".equals(s)) {
      return ZERO;
    }
    Matcher matcher = PATTERN.matcher(s);
    checkArgument(matcher.matches(), "Invalid ShortDuration: %s", s);

    BigDecimal value = new BigDecimal(matcher.group(1));
    String abbrev = matcher.group(2);
    TimeUnit unit = ABBREV_TO_UNIT.get(abbrev);
    checkArgument(unit != null, "Unrecognized time unit: %s", abbrev);

    return of(value, unit);
  }

  public static ShortDuration zero() {
    return ZERO;
  }

  // fortunately no abbreviation starts with 'e', so this should work
  private static final Pattern PATTERN = Pattern.compile("^([0-9.eE+-]+) ?(\\S+)$");

  private static ShortDuration ofPicos(long picos) {
    if (picos == 0) {
      return ZERO;
    }
    checkArgument(picos > 0);
    return new PositiveShortDuration(picos);
  }

  // TODO(kevinb): we sure seem to convert back and forth with BigDecimal a lot.
  // Why not just *make* this a BigDecimal?
  final long picos;

  ShortDuration(long picos) {
    this.picos = picos;
  }

  public long toPicos() {
    return picos;
  }

  public long to(TimeUnit unit) {
    return to(unit, RoundingMode.HALF_UP);
  }

  public abstract long to(TimeUnit unit, RoundingMode roundingMode);

  /*
   * In Guava, this will probably implement an interface called Quantity, and the following methods
   * will come from there, so they won't have to be defined here.
   */

  /**
   * Returns an instance of this type that represents the sum of this value and {@code
   * addend}.
   */
  public abstract ShortDuration plus(ShortDuration addend);

  /**
   * Returns an instance of this type that represents the difference of this value and
   * {@code subtrahend}.
   */
  public abstract ShortDuration minus(ShortDuration subtrahend);

  /**
   * Returns an instance of this type that represents the product of this value and the
   * integral value {@code multiplicand}.
   */
  public abstract ShortDuration times(long multiplicand);

  /**
   * Returns an instance of this type that represents the product of this value and {@code
   * multiplicand}, rounded according to {@code roundingMode} if necessary.
   *
   * <p>If this class represents an amount that is "continuous" rather than discrete, the
   * implementation of this method may simply ignore the rounding mode.
   */
  public abstract ShortDuration times(BigDecimal multiplicand, RoundingMode roundingMode);

  /**
   * Returns an instance of this type that represents this value divided by the integral
   * value {@code divisor}, rounded according to {@code roundingMode} if necessary.
   *
   * <p>If this class represents an amount that is "continuous" rather than discrete, the
   * implementation of this method may simply ignore the rounding mode.
   */
  public abstract ShortDuration dividedBy(long divisor, RoundingMode roundingMode);

  /**
   * Returns an instance of this type that represents this value divided by {@code
   * divisor}, rounded according to {@code roundingMode} if necessary.
   *
   * <p>If this class represents an amount that is "continuous" rather than discrete, the
   * implementation of this method may simply ignore the rounding mode.
   */
  public abstract ShortDuration dividedBy(BigDecimal divisor, RoundingMode roundingMode);

  // Zero

  private static ShortDuration ZERO = new ShortDuration(0) {
    @Override public long to(TimeUnit unit, RoundingMode roundingMode) {
      return 0;
    }
    @Override public ShortDuration plus(ShortDuration addend) {
      return addend;
    }
    @Override public ShortDuration minus(ShortDuration subtrahend) {
      checkArgument(this == subtrahend);
      return this;
    }
    @Override public ShortDuration times(long multiplicand) {
      return this;
    }
    @Override public ShortDuration times(BigDecimal multiplicand, RoundingMode roundingMode) {
      return this;
    }
    @Override public ShortDuration dividedBy(long divisor, RoundingMode roundingMode) {
      return dividedBy(new BigDecimal(divisor), roundingMode);
    }
    @Override public ShortDuration dividedBy(BigDecimal divisor, RoundingMode roundingMode) {
      checkArgument(divisor.compareTo(BigDecimal.ZERO) != 0);
      return this;
    }
    @Override public int compareTo(ShortDuration that) {
      if (this == that) {
        return 0;
      }
      checkNotNull(that);
      return -1;
    }
    @Override public boolean equals(@Nullable Object that) {
      return this == that;
    }
    @Override public int hashCode() {
      return 0;
    }
    @Override public String toString() {
      return "0s";
    }
  };

  // Non-zero

  private static class PositiveShortDuration extends ShortDuration {
    private PositiveShortDuration(long picos) {
      super(picos);
      checkArgument(picos > 0);
    }

    @Override public long to(TimeUnit unit, RoundingMode roundingMode) {
      BigDecimal divisor = ONE_IN_PICOS.get(unit);
      return toLong(new BigDecimal(picos).divide(divisor), roundingMode);
    }

    @Override public ShortDuration plus(ShortDuration addend) {
      return new PositiveShortDuration(picos + addend.picos);
    }

    @Override public ShortDuration minus(ShortDuration subtrahend) {
      return ofPicos(picos - subtrahend.picos);
    }

    @Override public ShortDuration times(long multiplicand) {
      if (multiplicand == 0) {
        return ZERO;
      }
      checkArgument(multiplicand >= 0, "negative multiplicand: %s", multiplicand);
      checkArgument(multiplicand <= Long.MAX_VALUE / picos,
          "product of %s and %s would overflow", this, multiplicand);
      return new PositiveShortDuration(picos * multiplicand);
    }

    @Override public ShortDuration times(BigDecimal multiplicand, RoundingMode roundingMode) {
      BigDecimal product = BigDecimal.valueOf(picos).multiply(multiplicand);
      return ofPicos(toLong(product, roundingMode));
    }

    @Override public ShortDuration dividedBy(long divisor, RoundingMode roundingMode) {
      return dividedBy(new BigDecimal(divisor), roundingMode);
    }

    @Override public ShortDuration dividedBy(BigDecimal divisor, RoundingMode roundingMode) {
      BigDecimal product = BigDecimal.valueOf(picos).divide(divisor, roundingMode);
      return ofPicos(product.longValueExact());
    }

    @Override public int compareTo(ShortDuration that) {
      return Longs.compare(this.picos, that.picos);
    }

    @Override public boolean equals(Object object) {
      if (object instanceof PositiveShortDuration) {
        PositiveShortDuration that = (PositiveShortDuration) object;
        return this.picos == that.picos;
      }
      return false;
    }

    @Override public int hashCode() {
      return Longs.hashCode(picos);
    }

    @Override public String toString() {
      TimeUnit bestUnit = TimeUnit.NANOSECONDS;
      for (TimeUnit unit : TimeUnit.values()) {
        if (picosIn(unit) > picos) {
          break;
        }
        bestUnit = unit;
      }
      BigDecimal divisor = ONE_IN_PICOS.get(bestUnit);

      return new BigDecimal(picos).divide(divisor, ROUNDER) + preferredAbbrev(bestUnit);
    }

    private static final MathContext ROUNDER = new MathContext(4);
  }

  // Private parts

  private static String preferredAbbrev(TimeUnit bestUnit) {
    return ABBREVIATIONS.get(bestUnit).get(0);
  }

  private static final ImmutableListMultimap<TimeUnit, String> ABBREVIATIONS =
      createAbbreviations();

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

  private static final Map<TimeUnit, BigDecimal> ONE_IN_PICOS = createUnitToPicosMap();

  private static Map<TimeUnit, BigDecimal> createUnitToPicosMap() {
    Map<TimeUnit, BigDecimal> map = Maps.newEnumMap(TimeUnit.class);
    for (TimeUnit unit : TimeUnit.values()) {
      map.put(unit, new BigDecimal(picosIn(unit)));
    }
    return Collections.unmodifiableMap(map);
  }

  private static final Map<TimeUnit, Long> MAXES = createMaxesMap();

  private static Map<TimeUnit, Long> createMaxesMap() {
    Map<TimeUnit, Long> map = Maps.newEnumMap(TimeUnit.class);
    for (TimeUnit unit : TimeUnit.values()) {
      // Max is 100 days
      map.put(unit, unit.convert(100L * 24 * 60 * 60, TimeUnit.SECONDS));
    }
    return Collections.unmodifiableMap(map);
  }

  private static long toLong(BigDecimal bd, RoundingMode roundingMode) {
    // setScale does not really mutate the BigDecimal
    return bd.setScale(0, roundingMode).longValueExact();
  }

  private static long picosIn(TimeUnit unit) {
    return unit.toNanos(1000);
  }
}
