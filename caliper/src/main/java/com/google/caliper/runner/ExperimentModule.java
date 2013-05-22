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
import com.google.caliper.model.BenchmarkSpec;
import com.google.caliper.util.Parser;
import com.google.caliper.util.Parsers;
import com.google.common.collect.ImmutableSortedMap;
import com.google.inject.AbstractModule;
import com.google.inject.MembersInjector;
import com.google.inject.TypeLiteral;
import com.google.inject.matcher.AbstractMatcher;
import com.google.inject.spi.TypeEncounter;
import com.google.inject.spi.TypeListener;

import java.lang.reflect.Field;
import java.text.ParseException;

/**
 * A module that binds data specific to a single experiment.
 */
public final class ExperimentModule extends AbstractModule {
  private final Class<?> benchmarkClass;
  private final ImmutableSortedMap<String, String> parameters;

  private ExperimentModule(Class<?> benchmarkClass, ImmutableSortedMap<String, String> parameters) {
    this.benchmarkClass = checkNotNull(benchmarkClass);
    this.parameters = checkNotNull(parameters);
  }

  public static ExperimentModule forExperiment(Experiment experiment) {
    return new ExperimentModule(experiment.instrumentation().benchmarkMethod().getDeclaringClass(),
        experiment.userParameters());
  }

  public static ExperimentModule forBenchmarkSpec(BenchmarkSpec spec)
      throws ClassNotFoundException {
    return new ExperimentModule(Class.forName(spec.className()), spec.parameters());
  }

  @Override protected void configure() {
    binder().requireExplicitBindings();
    bind(benchmarkClass);  // TypeListener doesn't fire without this
    bind(Object.class).annotatedWith(Running.Benchmark.class).to(benchmarkClass);
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
}
