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

package com.google.caliper.runner;

import com.google.caliper.api.ResultProcessor;
import com.google.caliper.runner.config.CaliperConfig;
import com.google.common.collect.ImmutableSet;
import dagger.Binds;
import dagger.MapKey;
import dagger.Module;
import dagger.Provides;
import dagger.multibindings.IntoMap;
import java.util.Map;
import javax.inject.Provider;

/** Configures the {@link ResultProcessor}s for a Caliper run. */
@Module
abstract class ResultProcessorModule {

  /**
   * Specifies the {@link Class} object to use as a key in the map of available {@link
   * ResultProcessor result processors} passed to {@link #provideResultProcessors(CaliperConfig,
   * Map)}.
   */
  @MapKey(unwrapValue = true)
  public @interface ResultProcessorClassKey {
    Class<? extends ResultProcessor> value();
  }

  @Binds
  @IntoMap
  @ResultProcessorClassKey(OutputFileDumper.class)
  abstract ResultProcessor bindOutputFileDumper(OutputFileDumper impl);

  @Binds
  @IntoMap
  @ResultProcessorClassKey(HttpUploader.class)
  abstract ResultProcessor bindHttpUploader(HttpUploader impl);

  @Provides
  static ImmutableSet<ResultProcessor> provideResultProcessors(
      CaliperConfig config,
      Map<Class<? extends ResultProcessor>, Provider<ResultProcessor>> availableProcessors) {
    ImmutableSet.Builder<ResultProcessor> builder = ImmutableSet.builder();
    for (Class<? extends ResultProcessor> processorClass : config.getConfiguredResultProcessors()) {
      Provider<ResultProcessor> resultProcessorProvider = availableProcessors.get(processorClass);
      ResultProcessor resultProcessor =
          resultProcessorProvider == null
              ? ResultProcessorCreator.createResultProcessor(processorClass)
              : resultProcessorProvider.get();
      builder.add(resultProcessor);
    }
    return builder.build();
  }
}
