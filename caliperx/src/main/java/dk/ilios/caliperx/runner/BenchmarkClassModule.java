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

package dk.ilios.caliperx.runner;

import dk.ilios.caliperx.runner.Running.AfterExperimentMethods;
import dk.ilios.caliperx.runner.Running.BeforeExperimentMethods;
import com.google.common.collect.ImmutableSet;
import com.google.inject.AbstractModule;
import com.google.inject.Key;
import com.google.inject.Provides;

import java.lang.reflect.Method;

import javax.inject.Singleton;

/**
 * Binds objects related to a benchmark class.
 */
// TODO(gak): move more of benchmark class into this module
public final class BenchmarkClassModule extends AbstractModule {
  private final Class<?> benchmarkClassObject;

  public BenchmarkClassModule(Class<?> benchmarkClassObject) {
    this.benchmarkClassObject = benchmarkClassObject;
  }

  @Override protected void configure() {
    bind(new Key<Class<?>>(Running.BenchmarkClass.class) {}).toInstance(benchmarkClassObject);
  }

  @Provides @Singleton BenchmarkClass provideBenchmarkClass() throws InvalidBenchmarkException {
    return BenchmarkClass.forClass(benchmarkClassObject);
  }

  @Provides @BeforeExperimentMethods ImmutableSet<Method> provideBeforeExperimentMethods(
      BenchmarkClass benchmarkClass) {
    return benchmarkClass.beforeExperimentMethods();
  }

  @Provides @AfterExperimentMethods ImmutableSet<Method> provideAfterExperimentMethods(
      BenchmarkClass benchmarkClass) {
    return benchmarkClass.afterExperimentMethods();
  }
}
