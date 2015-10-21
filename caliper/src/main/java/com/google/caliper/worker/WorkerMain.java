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

import com.google.caliper.bridge.CommandLineSerializer;
import com.google.caliper.bridge.OpenedSocket;
import com.google.caliper.bridge.ShouldContinueMessage;
import com.google.caliper.bridge.WorkerSpec;
import com.google.caliper.runner.ExperimentModule;
import com.google.common.net.InetAddresses;

import java.net.InetSocketAddress;
import java.nio.channels.SocketChannel;

/**
 * This class is invoked as a subprocess by the Caliper runner parent process; it re-stages
 * the benchmark and hands it off to the instrument's worker.
 */
public final class WorkerMain {
  private WorkerMain() {}

  public static void main(String[] args) throws Exception {
    // TODO(lukes): instead of parsing the spec from the command line pass the port number on the
    // command line and then receive the spec from the socket.  This way we can start JVMs prior
    // to starting experiments and thus get better experiment latency.
    WorkerSpec request = CommandLineSerializer.parse(args[0]);
    // nonblocking connect so we can interleave the system call with injector creation.
    SocketChannel channel = SocketChannel.open();
    channel.configureBlocking(false);
    channel.connect(new InetSocketAddress(InetAddresses.forString("127.0.0.1"), request.port));

    WorkerComponent workerComponent = DaggerWorkerComponent.builder()
        .experimentModule(ExperimentModule.forWorkerSpec(request))
        .workerModule(new WorkerModule(request))
        .build();
    Worker worker = workerComponent.getWorker();
    WorkerEventLog log = new WorkerEventLog(OpenedSocket.fromSocket(channel));

    log.notifyWorkerStarted(request.trialId);
    try {
      worker.setUpBenchmark();
      log.notifyBootstrapPhaseStarting();
      worker.bootstrap();
      log.notifyMeasurementPhaseStarting();
      boolean keepMeasuring = true;
      boolean isInWarmup = true;
      while (keepMeasuring) {
        worker.preMeasure(isInWarmup);
        log.notifyMeasurementStarting();
        try {
          ShouldContinueMessage message = log.notifyMeasurementEnding(worker.measure());
          keepMeasuring = message.shouldContinue();
          isInWarmup = !message.isWarmupComplete();
        } finally {
          worker.postMeasure();
        }
      }
    } catch (Exception e) {
      log.notifyFailure(e);
    } finally {
      System.out.flush(); // ?
      worker.tearDownBenchmark();
      log.close();
    }
  }
}
