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
package com.google.caliper.worker;

import static com.google.common.base.Preconditions.checkArgument;

import com.google.caliper.model.Measurement;
import com.google.caliper.model.Value;
import com.google.common.base.Objects;
import com.google.common.collect.ImmutableList;

import java.util.List;

/**
 * A set of statistics about the allocations performed by a benchmark method.
 */
class AllocationStats {
  private final int allocationCount;
  private final long allocationSize;
  private final int reps;

  /**
   * Constructs a new {@link AllocationStats} with the given number of allocations 
   * ({@code allocationCount}), cumulative size of the allocations ({@code allocationSize}) and the
   * number of {@code reps} passed to the bencmark method.
   */
  AllocationStats(int allocationCount, long allocationSize, int reps) {
    checkArgument(allocationCount >= 0, "allocationCount (%s) was negative", allocationCount);
    this.allocationCount = allocationCount;
    checkArgument(allocationSize >= 0, "allocationSize (%s) was negative", allocationSize);
    this.allocationSize = allocationSize;
    checkArgument(reps >= 0, "reps (%s) was negative", reps);
    this.reps = reps;
  }
  
  /**
   * Computes and returns the difference between this measurement and the given 
   * {@code baseline} measurement. The {@code baseline} measurement must have a lower weight 
   * (fewer reps) than this measurement.
   */
  AllocationStats minus(AllocationStats baseline) {
    try {
      return new AllocationStats(allocationCount - baseline.allocationCount,
            allocationSize - baseline.allocationSize,
            reps - baseline.reps);
    } catch (IllegalArgumentException e) {
      throw new IllegalStateException(String.format(
          "The difference between the baseline (%s) and the measurement (%s) is invalid",
              baseline, this), e);
    }
  }
  
  /**
   * Returns a list of {@link Measurement measurements} based on this collection of stats.
   */
  ImmutableList<Measurement> toMeasurements() {
    return ImmutableList.of(
        new Measurement.Builder()
            .value(Value.create(allocationCount, ""))
            .description("objects")
            .weight(reps)
            .build(),
        new Measurement.Builder()
            .value(Value.create(allocationSize, "B"))
            .weight(reps)
            .description("bytes")
            .build());
  }
  
  @Override public String toString() {
    return Objects.toStringHelper(this)
        .add("allocationCount", allocationCount)
        .add("allocationSize", allocationSize)
        .add("reps", reps)
        .toString();
  }
}