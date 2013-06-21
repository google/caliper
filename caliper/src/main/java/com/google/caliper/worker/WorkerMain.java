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

import static com.google.inject.Stage.PRODUCTION;

import com.google.caliper.bridge.BridgeModule;
import com.google.caliper.bridge.WorkerSpec;
import com.google.caliper.json.GsonModule;
import com.google.caliper.runner.ExperimentModule;
import com.google.caliper.runner.Running;
import com.google.common.collect.ImmutableList;
import com.google.gson.Gson;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Key;

import java.lang.reflect.Method;

/**
 * This class is invoked as a subprocess by the Caliper runner parent process; it re-stages
 * the benchmark and hands it off to the instrument's worker.
 */
public final class WorkerMain {
  private WorkerMain() {}

  public static void main(String[] args) throws Exception {
    Injector gsonInjector = Guice.createInjector(PRODUCTION, new GsonModule());
    WorkerSpec request =
        gsonInjector.getInstance(Gson.class).fromJson(args[0], WorkerSpec.class);

    Injector workerInjector = gsonInjector.createChildInjector(
        ExperimentModule.forBenchmarkSpec(request.benchmarkSpec),
        new BridgeModule(),
        new WorkerModule(request));

    Worker worker = workerInjector.getInstance(Worker.class);
    Object benchmark = workerInjector.getInstance(Key.get(Object.class, Running.Benchmark.class));
    WorkerEventLog log = workerInjector.getInstance(WorkerEventLog.class);

    log.notifyWorkerStarted();

    Method benchmarkMethod = findBenchmarkMethod(benchmark.getClass(), 
        request.benchmarkSpec.methodName(), 
        request.methodParameterClassNames);
    benchmarkMethod.setAccessible(true);
    try {
      runSetUp(benchmark);

      worker.measure(benchmark, benchmarkMethod, request.workerOptions, log);
    } catch (Exception e) {
      log.notifyFailure(e);
    } finally {
      System.out.flush(); // ?
      runTearDown(benchmark);
    }
  }

  private static Method findBenchmarkMethod(Class<?> benchmark, String methodName, 
      ImmutableList<String> methodParameterClassNames) {
    // Annoyingly Class.forName doesn't work for primitives so we can't convert these classnames
    // back into Class objects in order to call getDeclaredMethod(String, Class<?>...classes).
    // Instead we just match on names which should be just as unique.
    Method found = null;
    for (Method method : benchmark.getDeclaredMethods()) {
      if (method.getName().equals(methodName)) {
        if (methodParameterClassNames.equals(toClassNames(method.getParameterTypes()))) {
          if (found == null) {
            found = method;
          } else {
            throw new AssertionError(String.format(
                "Found two methods named %s with the same list of parameters: %s", 
                methodName, 
                methodParameterClassNames));
          }
        }
      }
    }
    if (found == null) {
      throw new AssertionError(String.format(
          "Could not find method %s in class %s with these parameters %s", 
          methodName,
          benchmark,
          methodParameterClassNames));
    }
    return found;
  }
  
  private static ImmutableList<String> toClassNames(Class<?>[] classes) {
    ImmutableList.Builder<String> classNames = ImmutableList.builder();
    for (Class<?> parameterType : classes) {
      classNames.add(parameterType.getName());
    }
    return classNames.build();
  }

  private static void runSetUp(Object benchmark) throws Exception {
    runBenchmarkMethod(benchmark, "setUp");
  }

  private static void runTearDown(Object benchmark) throws Exception {
    runBenchmarkMethod(benchmark, "tearDown");
  }

  private static void runBenchmarkMethod(Object benchmark, String methodName) throws Exception {
    // benchmark.setUp() or .tearDown() -- but they're protected
    try {
      Method method = benchmark.getClass().getDeclaredMethod(methodName);
      method.setAccessible(true);
      method.invoke(benchmark);
    } catch (NoSuchMethodException e) {
      // no method, don't invoke
    }
  }
}
