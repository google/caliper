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

package com.google.caliper.worker;

import com.google.caliper.InterleavedReader;
import com.google.caliper.api.Benchmark;
import com.google.caliper.model.Measurement;
import com.google.caliper.util.Parser;
import com.google.caliper.util.Parsers;
import com.google.caliper.util.Util;
import com.google.common.collect.ImmutableMap;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Collection;

/**
 * This class is invoked as a subprocess by the Caliper runner parent process; it re-stages
 * the benchmark and hands it off to the instrument's worker.
 */
public final class WorkerMain {
  private WorkerMain() {}

  public static void main(String[] args) throws Exception {
    WorkerRequest request = Util.GSON.fromJson(args[0], WorkerRequest.class);

    Class<?> benchmarkClass = Class.forName(request.benchmarkClassName);
    Benchmark benchmark = (Benchmark) construct(benchmarkClass);

    ImmutableMap<String, String> parameters = ImmutableMap.<String, String>builder()
        .putAll(request.injectedParameters)
        .putAll(request.vmArguments)
        .build();
    for (String fieldName : parameters.keySet()) {
      Field field = benchmarkClass.getDeclaredField(fieldName);
      field.setAccessible(true);
      Parser<?> parser = Parsers.conventionalParser(field.getType());
      field.set(benchmark, parser.parse(parameters.get(fieldName)));
    }
    Worker worker = (Worker) construct(request.workerClassName);
    WorkerEventLog log = new WorkerEventLog();

    try {
      runSetUp(benchmark);

      Collection<Measurement> measurements =
          worker.measure(benchmark, request.benchmarkMethodName, request.instrumentOptions, log);

      System.out.println(InterleavedReader.DEFAULT_MARKER + new WorkerResponse(measurements));
      System.out.flush(); // ?
    } finally {
      runTearDown(benchmark);
    }
  }

  private static Object construct(String className) throws Exception {
    return construct(Class.forName(className));
  }

  private static Object construct(Class<?> aClass) throws Exception {
    Constructor<?> constructor = aClass.getDeclaredConstructor();
    constructor.setAccessible(true);
    return constructor.newInstance();
  }

  private static void runSetUp(Benchmark benchmark) throws Exception {
    runBenchmarkMethod(benchmark, "setUp");
  }

  private static void runTearDown(Benchmark benchmark) throws Exception {
    runBenchmarkMethod(benchmark, "tearDown");
  }

  private static void runBenchmarkMethod(Benchmark benchmark, String methodName) throws Exception {
    // benchmark.setUp() or .tearDown() -- but they're protected
    Method method = Benchmark.class.getDeclaredMethod(methodName);
    method.setAccessible(true);
    method.invoke(benchmark);
  }
}
