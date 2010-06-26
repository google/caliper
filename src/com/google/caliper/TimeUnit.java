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

import com.google.common.collect.ImmutableSet;
import java.util.List;
import java.util.Set;

public class TimeUnit implements Comparable<TimeUnit> {
  private final String name;
  private final int exponent;
  private static final Set<TimeUnit> options = ImmutableSet.of(
    new TimeUnit("s", 0),
    new TimeUnit("ms", 3),
    new TimeUnit("us", 6),
    new TimeUnit("ns", 9)
  );

  public static Set<TimeUnit> getOptions() {
    return options;
  }

  TimeUnit(String name, int exponent) {
    this.name = name;
    this.exponent = exponent;
  }

  @Override
  public String toString() {
    return name;
  }

  public Integer getExponent() {
    return exponent;
  }

  @Override
  public int compareTo(TimeUnit o) {
    return getExponent().compareTo(o.getExponent());
  }
}
