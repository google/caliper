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

import static com.google.common.base.Preconditions.checkNotNull;

import com.google.caliper.api.Benchmark;
import com.google.caliper.spi.Instrument;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableSetMultimap;
import com.google.common.collect.ImmutableSortedMap;

import java.lang.reflect.Method;

/**
 * An instance of this type represents a user-provided class that extends Benchmark.
 */
public final class BenchmarkClass {
  public static BenchmarkClass forName(String name) {
    Class<?> aClass = null;
    try {
      aClass = Class.forName(name);
    } catch (ClassNotFoundException e) {
      throw new IllegalArgumentException("Class not found: " + name);
    }
    if (!Benchmark.class.isAssignableFrom(aClass)) {
      throw new IllegalArgumentException("Does not extend Benchmark: " + name);
    }
    return new BenchmarkClass(aClass.asSubclass(Benchmark.class));
  }

  private final Class<? extends Benchmark> theClass;
  private final ImmutableSet<Object /*Parameter*/> parameters;

  public BenchmarkClass(Class<? extends Benchmark> theClass) {
    this.theClass = checkNotNull(theClass);
    try {
      theClass.newInstance();
    } catch (InstantiationException e) {
      throw new IllegalArgumentException("Could not instantiate: " + theClass.getName());
    } catch (IllegalAccessException e) {
      throw new IllegalArgumentException(
          "Not public or lacks public constructor: " + theClass.getName());
    }

    this.parameters = ImmutableSet.of(); // TODO: find them all
  }

  public ImmutableSortedMap<String, BenchmarkMethod> findAllBenchmarkMethods(Instrument instrument) {
    ImmutableSortedMap.Builder<String, BenchmarkMethod> result = ImmutableSortedMap.naturalOrder();
    for (Method method : theClass.getDeclaredMethods()) {
      if (instrument.isBenchmarkMethod(method)) {
        BenchmarkMethod benchmarkMethod = instrument.createBenchmarkMethod(this, method);
        result.put(benchmarkMethod.name(), benchmarkMethod);
      }
    }
    return result.build();
  }

  public ImmutableSetMultimap<String, String> resolveUserParameters(
      ImmutableSetMultimap<String, String> commandLineOverrides) {
    ImmutableSetMultimap.Builder<String, String> mergedParams = ImmutableSetMultimap.builder();
    for (Object parameter : parameters) {
      
    }
    return null;
  }

  @Override public boolean equals(Object object) {
    if (object instanceof BenchmarkClass) {
      BenchmarkClass that = (BenchmarkClass) object;
      return this.theClass.equals(that.theClass);
    }
    return false;
  }

  @Override public int hashCode() {
    return theClass.hashCode();
  }

  @Override public String toString() {
    return theClass.getName();
  }
}
