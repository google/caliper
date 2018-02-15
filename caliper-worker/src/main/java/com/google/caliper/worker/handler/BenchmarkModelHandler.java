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
import com.google.caliper.core.Running.BenchmarkClass;
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
  private final Class<?> benchmarkClass;

  @Inject
  BenchmarkModelHandler(
      ClientConnectionService clientConnection, @BenchmarkClass Class<?> benchmarkClass) {
    this.clientConnection = clientConnection;
    this.benchmarkClass = benchmarkClass;
  }

  @Override
  public void handleRequest(WorkerRequest request) throws IOException {
    BenchmarkModelRequest modelRequest = (BenchmarkModelRequest) request;
    BenchmarkClassModel model = BenchmarkClassModel.create(benchmarkClass);
    BenchmarkClassModel.validateUserParameters(benchmarkClass, modelRequest.userParameters());
    clientConnection.send(BenchmarkModelLogMessage.create(model));
  }
}
