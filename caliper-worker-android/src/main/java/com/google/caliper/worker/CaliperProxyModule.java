/*
 * Copyright (C) 2018 Google Inc.
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

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.PARAMETER;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import dagger.Module;
import dagger.Provides;
import java.lang.annotation.Annotation;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import javax.inject.Qualifier;
import javax.inject.Singleton;

/** Module with bindings for creating a {@link CaliperProxy}. */
@Module
abstract class CaliperProxyModule {

  /** Qualifier for native library path string. */
  @Retention(RUNTIME)
  @Target({FIELD, PARAMETER, METHOD})
  @Qualifier
  public @interface NativeLibraryDir {
    Class<? extends Annotation> value() default NativeLibraryDir.class;
  }

  private CaliperProxyModule() {}

  @Provides
  @Singleton
  static ExecutorService executor() {
    return Executors.newCachedThreadPool();
  }
}
