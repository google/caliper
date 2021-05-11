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

import com.google.caliper.worker.CaliperProxyModule.NativeLibraryDir;
import com.google.common.collect.ImmutableMap;
import dagger.BindsInstance;
import dagger.Component;
import java.net.InetSocketAddress;
import java.util.UUID;
import javax.inject.Singleton;

/** Component for creating a {@link CaliperProxy}. */
@Singleton
@Component(modules = CaliperProxyModule.class)
interface CaliperProxyComponent {
  /** Gets the {@link CaliperProxy} instance. */
  CaliperProxy caliperProxy();

  @Component.Builder
  interface Builder {
    @BindsInstance
    Builder id(UUID id);

    @BindsInstance
    Builder classpath(String classpath);

    @BindsInstance
    Builder nativeLibraryDir(@NativeLibraryDir String nativeLibraryDir);

    @BindsInstance
    Builder processEnv(ImmutableMap<String, String> processEnv);

    @BindsInstance
    Builder clientAddress(InetSocketAddress address);

    CaliperProxyComponent build();
  }
}
