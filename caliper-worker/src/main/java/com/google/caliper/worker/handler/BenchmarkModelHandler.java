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

package com.google.caliper.worker.handler;

import com.google.caliper.bridge.BenchmarkModelLogMessage;
import com.google.caliper.bridge.BenchmarkModelRequest;
import com.google.caliper.bridge.WorkerRequest;
import com.google.caliper.core.BenchmarkClassModel;
import com.google.caliper.core.UserCodeException;
import com.google.caliper.util.InvalidCommandException;
import com.google.caliper.util.Util;
import com.google.caliper.worker.connection.ClientConnectionService;
import java.io.IOException;
import javax.inject.Inject;

/**
 * Handler for a {@link BenchmarkModelRequest}.
 *
 * @author Colin Decker
 */
final class BenchmarkModelHandler implements RequestHandler {

  private final ClientConnectionService clientConnection;

  @Inject
  BenchmarkModelHandler(ClientConnectionService clientConnection) {
    this.clientConnection = clientConnection;
  }

  @Override
  public void handleRequest(WorkerRequest request) throws IOException {
    BenchmarkModelRequest modelRequest = (BenchmarkModelRequest) request;
    Class<?> benchmarkClass = getClass(modelRequest.benchmarkClass());
    BenchmarkClassModel model = BenchmarkClassModel.create(benchmarkClass);
    BenchmarkClassModel.validateUserParameters(benchmarkClass, modelRequest.userParameters());
    clientConnection.send(BenchmarkModelLogMessage.create(model));
  }

  private static Class<?> getClass(String className) {
    try {
      return Util.lenientClassForName(className);
    } catch (ClassNotFoundException e) {
      throw new InvalidCommandException("Benchmark class not found: " + className);
    } catch (ExceptionInInitializerError e) {
      throw new UserCodeException(
          "Exception thrown while initializing class: " + className, e.getCause());
    } catch (NoClassDefFoundError e) {
      throw new UserCodeException("Unable to load class: " + className, e);
    }
  }
}
