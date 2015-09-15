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

import com.google.common.util.concurrent.Service;
import com.google.common.util.concurrent.ServiceManager;
import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.multibindings.Multibinder;

import java.util.Set;

import javax.inject.Singleton;

/** Configures the {@link ServiceManager}. */
class ServiceModule extends AbstractModule {
  @Override protected void configure() {
    // Ensure that the binding exists, even if it is empty.
    Multibinder.newSetBinder(binder(), Service.class);
  }

  @Provides
  @Singleton
  ServiceManager provideServiceManager(Set<Service> services) {
    return new ServiceManager(services);
  }
}
