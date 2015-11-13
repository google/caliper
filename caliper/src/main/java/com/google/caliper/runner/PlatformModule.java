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

import com.google.caliper.platform.Platform;
import com.google.caliper.platform.dalvik.DalvikModule;
import com.google.caliper.platform.dalvik.DalvikPlatform;
import com.google.caliper.platform.jvm.JvmModule;
import com.google.caliper.platform.jvm.JvmPlatform;
import com.google.common.base.Optional;

import dagger.Module;
import dagger.Provides;

import javax.inject.Provider;

/**
 * Provider of a {@link Platform} instance appropriate for the current platform.
 */
@Module(includes = {JvmModule.class, DalvikModule.class})
public final class PlatformModule {

  /**
   * Chooses the {@link DalvikPlatform} if available, otherwise uses the default
   * {@link JvmPlatform}.
   */
  @Provides
  static Platform providePlatform(
      Optional<DalvikPlatform> optionalDalvikPlatform,
      Provider<JvmPlatform> jvmPlatformProvider) {
    if (optionalDalvikPlatform.isPresent()) {
      return optionalDalvikPlatform.get();
    } else {
      return jvmPlatformProvider.get();
    }
  }
}
