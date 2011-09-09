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

import com.google.common.annotations.GwtCompatible;

@GwtCompatible
public class LinearTranslation {
  //  y = mx + b
  private final double m;
  private final double b;

  // TODO(kevinb): why so high? why even check this at all?
  private static final double EQUALITY_TOLERANCE = 1.0E-6;

  /**
   * Constructs a linear translation for which {@code translate(in1) == out1}
   * and {@code translate(in2) == out2}.
   *
   * @throws IllegalArgumentException if {@code in1 == in2}
   */
  public LinearTranslation(double in1, double out1, double in2, double out2) {
    if (Math.abs(in1 - in2) < EQUALITY_TOLERANCE) {
      throw new IllegalArgumentException("in1 and in2 are approximately equal");
    }
    double divisor = in1 - in2;
    this.m = (out1 - out2) / divisor;
    this.b = (in1 * out2 - in2 * out1) / divisor;
  }

  public double translate(double in) {
    return m * in + b;
  }
}
