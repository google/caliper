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

package com.google.caliper.worker;

import static com.google.common.base.Charsets.UTF_8;

import com.google.caliper.Param;
import com.google.caliper.api.Benchmark;
import com.google.caliper.bridge.WorkerSpec;
import com.google.caliper.util.Parser;
import com.google.caliper.util.Parsers;
import com.google.common.collect.ImmutableMap;
import com.google.inject.AbstractModule;
import com.google.inject.MembersInjector;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.google.inject.TypeLiteral;
import com.google.inject.matcher.AbstractMatcher;
import com.google.inject.spi.TypeEncounter;
import com.google.inject.spi.TypeListener;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.lang.reflect.Field;
import java.text.ParseException;

/**
 * Binds classes necessary for the worker. Also manages the injection of {@link Param parameters}
 * from the {@link WorkerSpec} into the {@link Benchmark}.
 */
final class WorkerModule extends AbstractModule {
  private final Class<? extends Benchmark> benchmarkClass;
  private final Class<? extends Worker> workerClass;
  private final ImmutableMap<String, String> parameters;
  private final String pipePath;

  @SuppressWarnings("unchecked")
  WorkerModule(WorkerSpec workerSpec) {
    try {
      this.workerClass = (Class<? extends Worker>) Class.forName(workerSpec.workerClassName);
      this.benchmarkClass =
          (Class<? extends Benchmark>) Class.forName(workerSpec.benchmarkSpec.className());
    } catch (ClassNotFoundException e) {
      throw new AssertionError("classes referenced in the runner are always present");
    }
    this.parameters = ImmutableMap.copyOf(workerSpec.benchmarkSpec.parameters());
    this.pipePath = workerSpec.pipePath;
  }

  @Override protected void configure() {
    bind(benchmarkClass);  // TypeListener doesn't fire without this
    bind(Benchmark.class).to(benchmarkClass);
    bind(Worker.class).to(workerClass);

    bindListener(new BenchmarkTypeMatcher(), new BenchmarkParameterInjector());
  }

  @Provides @Singleton PrintWriter providePrintWriter() throws FileNotFoundException {
    PrintWriter printWriter = new PrintWriter(
        new OutputStreamWriter(new FileOutputStream(new File(pipePath)), UTF_8), true);
    printWriter.println();  // prime the pipe
    printWriter.flush();
    return printWriter;
  }

  private static final class BenchmarkTypeMatcher extends AbstractMatcher<TypeLiteral<?>> {
    @Override
    public boolean matches(TypeLiteral<?> t) {
      return Benchmark.class.isAssignableFrom(t.getRawType());
    }
  }

  private final class BenchmarkParameterInjector implements TypeListener {
    @Override
    public <I> void hear(TypeLiteral<I> type, final TypeEncounter<I> encounter) {
      for (final Field field : type.getRawType().getDeclaredFields()) {
        if (field.isAnnotationPresent(Param.class)) {
          encounter.register(new MembersInjector<I>() {
            @Override public void injectMembers(I instance) {
              try {
                field.setAccessible(true);
                Parser<?> parser = Parsers.conventionalParser(field.getType());
                field.set(instance, parser.parse(parameters.get(field.getName())));
              } catch (NoSuchMethodException e) {
                encounter.addError(e);
              } catch (ParseException e) {
                encounter.addError(e);
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
