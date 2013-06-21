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

package com.google.caliper.runner;

import static com.google.common.base.Preconditions.checkNotNull;

import com.google.caliper.Param;
import com.google.caliper.bridge.WorkerSpec;
import com.google.caliper.util.Parser;
import com.google.caliper.util.Parsers;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSortedMap;
import com.google.inject.AbstractModule;
import com.google.inject.Key;
import com.google.inject.MembersInjector;
import com.google.inject.TypeLiteral;
import com.google.inject.matcher.AbstractMatcher;
import com.google.inject.spi.TypeEncounter;
import com.google.inject.spi.TypeListener;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.text.ParseException;

/**
 * A module that binds data specific to a single experiment.
 */
public final class ExperimentModule extends AbstractModule {
  private final Class<?> benchmarkClass;
  private final ImmutableSortedMap<String, String> parameters;
  private final Method benchmarkMethod;

  private ExperimentModule(Class<?> benchmarkClass, Method benchmarkMethod, 
      ImmutableSortedMap<String, String> parameters) {
    this.benchmarkClass = checkNotNull(benchmarkClass);
    this.parameters = checkNotNull(parameters);
    this.benchmarkMethod = benchmarkMethod;
  }

  public static ExperimentModule forExperiment(Experiment experiment) {
    Method benchmarkMethod = experiment.instrumentation().benchmarkMethod();
    return new ExperimentModule(benchmarkMethod.getDeclaringClass(), 
        benchmarkMethod,
        experiment.userParameters());
  }

  public static ExperimentModule forWorkerSpec(WorkerSpec spec) 
      throws ClassNotFoundException {
    Class<?> benchmarkClass = Class.forName(spec.benchmarkSpec.className());
    Method benchmarkMethod = findBenchmarkMethod(benchmarkClass, spec.benchmarkSpec.methodName(), 
        spec.methodParameterClassNames);
    benchmarkMethod.setAccessible(true);
    return new ExperimentModule(benchmarkClass, benchmarkMethod, spec.benchmarkSpec.parameters());
  }

  @Override protected void configure() {
    binder().requireExplicitBindings();
    bind(benchmarkClass);  // TypeListener doesn't fire without this
    bind(Object.class).annotatedWith(Running.Benchmark.class).to(benchmarkClass);
    bind(new Key<Class<?>>(Running.BenchmarkClass.class) {}).toInstance(benchmarkClass);
    bindConstant().annotatedWith(Running.BenchmarkMethod.class).to(benchmarkMethod.getName());
    bind(Method.class).annotatedWith(Running.BenchmarkMethod.class).toInstance(benchmarkMethod);
    bindListener(new BenchmarkTypeMatcher(), new BenchmarkParameterInjector());
  }

  private final class BenchmarkTypeMatcher extends AbstractMatcher<TypeLiteral<?>> {
    @Override
    public boolean matches(TypeLiteral<?> t) {
      return t.getType().equals(benchmarkClass);
    }
  }

  private final class BenchmarkParameterInjector implements TypeListener {
    @Override
    public <I> void hear(TypeLiteral<I> type, TypeEncounter<I> encounter) {
      for (final Field field : type.getRawType().getDeclaredFields()) {
        if (field.isAnnotationPresent(Param.class)) {
          encounter.register(new MembersInjector<I>() {
            @Override public void injectMembers(I instance) {
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
          });
        }
      }
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
}
