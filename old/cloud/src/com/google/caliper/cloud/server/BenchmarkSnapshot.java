/**
 * Copyright (C) 2010 Google Inc.
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

package com.google.caliper.cloud.server;

import com.google.caliper.cloud.client.Benchmark;
import com.google.caliper.cloud.client.BenchmarkSnapshotMeta;

public class BenchmarkSnapshot {
  private final Benchmark benchmark;
  private final BenchmarkSnapshotMeta metadata;

  public BenchmarkSnapshot(Benchmark benchmark, long created, long id) {
    this(benchmark, new BenchmarkSnapshotMeta(
        benchmark.getOwner(), benchmark.getName(), created, id));
  }
  
  public BenchmarkSnapshot(Benchmark benchmark, BenchmarkSnapshotMeta metadata) {
    this.benchmark = benchmark;
    this.metadata = metadata;
  }

  public Benchmark getBenchmark() {
    return benchmark;
  }

  public BenchmarkSnapshotMeta getMetadata() {
    return metadata;
  }
}
