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

package com.google.caliper.runner.worker.targetinfo;

import com.google.caliper.bridge.TargetInfoRequest;
import com.google.caliper.bridge.WorkerRequest;
import com.google.caliper.runner.options.CaliperOptions;
import com.google.caliper.runner.server.LocalPort;
import com.google.caliper.runner.target.Target;
import com.google.caliper.runner.worker.WorkerScoped;
import com.google.caliper.runner.worker.WorkerSpec;
import java.io.PrintWriter;
import java.util.UUID;
import javax.inject.Inject;

/** A {@link WorkerSpec} for getting target info from a target. */
@WorkerScoped
final class TargetInfoSpec extends WorkerSpec {

  private final Target target;
  private final CaliperOptions options;

  @Inject
  TargetInfoSpec(Target target, UUID id, @LocalPort int port, CaliperOptions options) {
    super(target, id, id, port, options.benchmarkClassName());
    this.target = target;
    this.options = options;
  }

  @Override
  public String name() {
    return "target-info-" + target.name();
  }

  @Override
  public WorkerRequest request() {
    return TargetInfoRequest.create(options.userParameters());
  }

  @Override
  public void printInfoHeader(PrintWriter writer) {
    writer.println("Worker Id: " + id());
    writer.println("Benchmark Class Name: " + options.benchmarkClassName());
  }
}
