/*
 * Copyright (C) 2011 Google Inc.
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

import com.google.caliper.bridge.FailureLogMessage;
import com.google.caliper.bridge.StartupAnnounceMessage;
import com.google.caliper.bridge.WorkerRequest;
import com.google.caliper.worker.connection.ClientConnectionService;
import com.google.caliper.worker.handler.RequestDispatcher;
import java.io.IOException;
import java.util.UUID;
import javax.inject.Inject;

/**
 * Main injectable class that {@code WorkerMain} classes delegate to to handle the full worker run.
 *
 * @author Colin Decker
 */
public final class Worker {

  private final UUID id;
  private final ClientConnectionService clientConnection;
  private final RequestDispatcher requestDispatcher;

  @Inject
  Worker(UUID id, ClientConnectionService clientConnection, RequestDispatcher requestDispatcher) {
    this.id = id;
    this.clientConnection = clientConnection;
    this.requestDispatcher = requestDispatcher;
  }

  /** Runs the worker. */
  public void run() throws IOException {
    clientConnection.startAsync().awaitRunning();
    try {
      clientConnection.send(new StartupAnnounceMessage(id));
      WorkerRequest request = (WorkerRequest) clientConnection.receive();
      requestDispatcher.dispatch(request);
    } catch (IOException e) {
      // If an IOException was thrown, it was probably from trying to send something to the
      // runner and failing, so don't bother trying to send *that* to the runner.
      throw e;
    } catch (Exception e) {
      clientConnection.send(FailureLogMessage.create(e));
    } finally {
      clientConnection.stopAsync().awaitTerminated();
    }
  }
}
