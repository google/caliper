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

package com.google.caliper.runner;

import com.google.common.base.Objects;

import java.lang.reflect.Method;

/**
 * A method of a benchmark class that is recognized (by the appropriate Instrument) as a method
 * that should undergo benchmark testing; for example, for the default instrument ("time"), methods
 * that begin with the string "time" are recognized.
 */
public final class BenchmarkMethod {
  private final BenchmarkClass benchmarkClass;
  private final String shortName;
  private final Method method;

  public BenchmarkMethod(BenchmarkClass benchmarkClass, Method method, String shortName) {
    this.benchmarkClass = benchmarkClass;
    this.shortName = shortName;
    this.method = method;
  }

  public String name() {
    return shortName;
  }

  public String className() {
    return benchmarkClass.name();
  }

  public Method method() {
    return method;
  }

  @Override public boolean equals(Object object) {
    if (object instanceof BenchmarkMethod) {
      BenchmarkMethod that = (BenchmarkMethod) object;

      // Who ensures that two can't have the same short name?
      return this.benchmarkClass.equals(that.benchmarkClass)
          && this.shortName.equals(that.shortName);
    }
    return false;
  }

  @Override public int hashCode() {
    return Objects.hashCode(benchmarkClass, shortName);
  }

  @Override public String toString() {
    return name();
  }
}
