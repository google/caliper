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

import com.google.caliper.api.Benchmark;
import com.google.caliper.util.InvalidCommandException;
import com.google.caliper.util.ShortDuration;
import com.google.caliper.util.Util;
import com.google.caliper.worker.Worker;
import com.google.common.collect.ImmutableMap;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Map;

public abstract class Instrument {
  static Instrument createInstrument(String instrumentName, CaliperRc rc)
      throws InvalidCommandException, UserCodeException {
    String instrumentClassName = rc.instrumentClassName(instrumentName);
    try {
      Class<?> someClass = Util.lenientClassForName(instrumentClassName);
      Class<? extends Instrument> instrumentClass = someClass.asSubclass(Instrument.class);
      Constructor<? extends Instrument> instrumentConstr = instrumentClass.getDeclaredConstructor();
      instrumentConstr.setAccessible(true);
      Instrument instrument = instrumentConstr.newInstance();
      instrument.setOptions(rc.instrumentOptions(instrumentName));
      return instrument;

    } catch (ClassNotFoundException e) {
      throw new InvalidCommandException(
          "Invalid instrument '%s'; cannot find class '%s'", instrumentName, instrumentClassName);

    } catch (ClassCastException e) {
      throw new InvalidInstrumentException(
          "Instrument class '%s' does not implement Instrument", instrumentClassName);

    } catch (NoSuchMethodException e) {
      throw new InvalidInstrumentException(
          "Instrument class '%s' has no parameterless constructor", instrumentClassName);

    } catch (InstantiationException e) {
      throw new InvalidInstrumentException("Instrument class '%s' couldn't be constructed",
          instrumentClassName);

    } catch (InvocationTargetException e) {
      throw new UserCodeException(
          "An exception was thrown when constructing the instrument", e.getCause());

    } catch (IllegalAccessException e) {
      throw new AssertionError(e);
    }
  }

  protected ImmutableMap<String, String> options;

  protected void setOptions(Map<String, String> options) {
    this.options = ImmutableMap.copyOf(options);
  }

  public ShortDuration estimateRuntimePerTrial() {
    throw new UnsupportedOperationException();
  }

  public abstract boolean isBenchmarkMethod(Method method);

  // TODO: make BenchmarkMethod more abstract, not necessarily tied persistently to a Method (even
  // though the presence of a particular method is what probably triggers its recognition/creation
  // in the first place?), and give it an invoke() method.

  public abstract BenchmarkMethod createBenchmarkMethod(
      BenchmarkClass benchmarkClass, Method method) throws InvalidBenchmarkException;

  public abstract void dryRun(Benchmark benchmark, BenchmarkMethod method) throws UserCodeException;

  public Map<String, String> workerOptions() {
    return options;
  }

  public abstract Class<? extends Worker> workerClass();
}
