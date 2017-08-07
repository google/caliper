/*
 * Copyright (C) 2017 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */

package com.google.caliper.platform;

import static com.google.common.base.Preconditions.checkNotNull;

import dagger.Module;
import dagger.Provides;

/**
 * Module for binding the current {@link Platform}.
 *
 * @author Colin Decker
 */
@Module
public final class PlatformModule {

  private final Platform platform;

  public PlatformModule(Platform platform) {
    this.platform = checkNotNull(platform);
  }

  @Provides // TODO(dpb): Replace with a @BindsInstance method on the component builder.
  Platform providePlatform() {
    return platform;
  }
}
