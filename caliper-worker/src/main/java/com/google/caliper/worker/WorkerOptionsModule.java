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

import static com.google.common.base.Preconditions.checkNotNull;

import com.google.caliper.core.Running.BenchmarkClass;
import com.google.caliper.core.UserCodeException;
import com.google.caliper.util.InvalidCommandException;
import com.google.caliper.util.Util;
import dagger.Module;
import dagger.Provides;
import dagger.Reusable;
import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.Arrays;
import java.util.Iterator;
import java.util.UUID;

/** Module for options for a Caliper worker that are passed to it on the command line. */
@Module
final class WorkerOptionsModule {

  /** Parses the given command line args to a {@link WorkerOptionsModule}. */
  public static WorkerOptionsModule fromArgs(String[] args) throws IOException {
    Iterator<String> iter = Arrays.asList(args).iterator();
    return new WorkerOptionsModule(
        UUID.fromString(iter.next()),
        new InetSocketAddress(InetAddress.getLocalHost(), Integer.parseInt(iter.next())),
        iter.next());
  }

  private final UUID id;
  private final InetSocketAddress clientAddress;
  private final String benchmarkClassName;

  private WorkerOptionsModule(UUID id, InetSocketAddress clientAddress, String benchmarkClassName) {
    this.id = checkNotNull(id);
    this.clientAddress = checkNotNull(clientAddress);
    this.benchmarkClassName = checkNotNull(benchmarkClassName);
  }

  /** Provides the ID of the worker. */
  @Provides
  UUID id() {
    return id;
  }

  /** Provides the address of the client the worker should open a connection to. */
  @Provides
  InetSocketAddress clientAddress() {
    return clientAddress;
  }

  /** Provides the benchmark class object. */
  @Provides
  @Reusable
  @BenchmarkClass
  Class<?> benchmarkClass() {
    try {
      return Util.lenientClassForName(benchmarkClassName);
    } catch (ClassNotFoundException e) {
      throw new InvalidCommandException("Benchmark class not found: " + benchmarkClassName);
    } catch (ExceptionInInitializerError e) {
      throw new UserCodeException(
          "Exception thrown while initializing class: " + benchmarkClassName, e.getCause());
    } catch (NoClassDefFoundError e) {
      throw new UserCodeException("Unable to load class: " + benchmarkClassName, e);
    }
  }
}
