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

import com.google.caliper.worker.connection.ClientAddress;
import com.google.caliper.worker.handler.RequestHandlerModule;
import dagger.Module;
import dagger.Provides;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.Random;
import java.util.UUID;
import javax.inject.Singleton;

/**
 * Module providing bindings needed by the {@link Worker}.
 *
 * @author Colin Decker
 */
@Module(includes = RequestHandlerModule.class)
abstract class WorkerModule {

  @Provides
  static UUID provideWorkerId(String[] args) {
    return UUID.fromString(args[0]);
  }

  @Provides
  @ClientAddress
  static InetSocketAddress provideClientAddress(String[] args) {
    int port = Integer.parseInt(args[1]);
    return new InetSocketAddress(InetAddress.getLoopbackAddress(), port);
  }

  @Provides
  @Singleton
  static Random provideRandom() {
    return new Random();
  }
}
