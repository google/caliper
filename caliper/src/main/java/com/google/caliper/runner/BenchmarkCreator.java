/*
 * Copyright (C) 2015 Google Inc.
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

import com.google.caliper.Param;
import com.google.caliper.util.Parser;
import com.google.caliper.util.Parsers;
import com.google.common.collect.ImmutableSortedMap;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.text.ParseException;

import javax.inject.Inject;

/**
 * Responsible for creating instances of the benchmark class.
 */
final class BenchmarkCreator {

  private static final String BENCHMARK_NO_PUBLIC_DEFAULT_CONSTRUCTOR =
      "Benchmark %s does not have a publicly visible default constructor";

  private final Class<?> benchmarkClass;
  private final ImmutableSortedMap<String, String> parameters;
  private final Constructor<?> benchmarkClassCtor;

  @Inject
  BenchmarkCreator(
      @Running.BenchmarkClass Class<?> benchmarkClass,
      @Running.Benchmark ImmutableSortedMap<String, String> parameters) {
    this.benchmarkClass = benchmarkClass;
    this.benchmarkClassCtor = findDefaultConstructor(benchmarkClass);
    this.parameters = parameters;
  }

  private static Constructor<?> findDefaultConstructor(Class<?> benchmarkClass) {
    Constructor<?> defaultConstructor = null;
    for (Constructor<?> constructor : benchmarkClass.getDeclaredConstructors()) {
      if (constructor.getParameterTypes().length == 0) {
        defaultConstructor = constructor;
        defaultConstructor.setAccessible(true);
        break;
      }
    }
    if (defaultConstructor == null) {
      throw new UserCodeException(
          String.format(BENCHMARK_NO_PUBLIC_DEFAULT_CONSTRUCTOR, benchmarkClass), null);
    }
    return defaultConstructor;
  }

  Object createBenchmarkInstance() {
    Object instance;
    try {
      instance = benchmarkClassCtor.newInstance();
    } catch (InstantiationException e) {
      throw new AssertionError(e);
    } catch (IllegalAccessException e) {
      throw new AssertionError(e);
    } catch (InvocationTargetException e) {
      Throwable userException = e.getCause();
      throw new UserCodeException(userException);
    }

    // Inject values for the user parameters.
    for (Field field : benchmarkClass.getDeclaredFields()) {
      if (field.isAnnotationPresent(Param.class)) {
        try {
          field.setAccessible(true);
          Parser<?> parser = Parsers.conventionalParser(field.getType());
          field.set(instance, parser.parse(parameters.get(field.getName())));
        } catch (NoSuchMethodException e) {
          throw new RuntimeException(e);
        } catch (ParseException e) {
          throw new RuntimeException(e);
        } catch (IllegalArgumentException e) {
          throw new AssertionError("types have been checked");
        } catch (IllegalAccessException e) {
          throw new AssertionError("already set access");
        }
      }
    }

    return instance;
  }
}
