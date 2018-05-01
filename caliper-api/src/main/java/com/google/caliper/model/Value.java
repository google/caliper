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

package com.google.caliper.model;

import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.base.Objects;
import java.io.Serializable;

/** A magnitude with units. */
public class Value implements Serializable {
  private static final long serialVersionUID = 1L;

  static final Value DEFAULT = new Value();

  public static Value create(double value, String unit) {
    return new Value(value, checkNotNull(unit));
  }

  private double magnitude;
  // TODO(gak): do something smarter than string for units
  // TODO(gak): give guidelines for how to specify units.  E.g. s or seconds
  private String unit;

  private Value() {
    this.magnitude = 0.0;
    this.unit = "";
  }

  private Value(double value, String unit) {
    this.magnitude = value;
    this.unit = unit;
  }

  public String unit() {
    return unit;
  }

  public double magnitude() {
    return magnitude;
  }

  @Override
  public boolean equals(Object obj) {
    if (obj == this) {
      return true;
    } else if (obj instanceof Value) {
      Value that = (Value) obj;
      return this.magnitude == that.magnitude && this.unit.equals(that.unit);
    } else {
      return false;
    }
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(magnitude, unit);
  }

  @Override
  public String toString() {
    return new StringBuilder().append(magnitude).append(unit).toString();
  }
}
