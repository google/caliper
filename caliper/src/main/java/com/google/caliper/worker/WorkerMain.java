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

    for (String fieldName : request.injectedParameters.keySet()) {
      Field field = benchmarkClass.getDeclaredField(fieldName);
      field.setAccessible(true);
      Parser<?> parser = Parsers.conventionalParser(field.getType());
      field.set(benchmark, parser.parse(request.injectedParameters.get(fieldName)));
    }
    Worker worker = (Worker) construct(request.workerClassName);
    WorkerEventLog log = new WorkerEventLog();

    runSetUp(benchmark);

    Collection<Measurement> measurements =
        worker.measure(benchmark, request.benchmarkMethodName, request.instrumentOptions, log);

    System.out.println(InterleavedReader.DEFAULT_MARKER + new WorkerResponse(measurements));
    System.out.flush(); // ?
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
    // benchmark.setUp() -- but oops, it's 'protected'
    Method method = Benchmark.class.getDeclaredMethod("setUp");
    method.setAccessible(true);
    method.invoke(benchmark);
  }
}
