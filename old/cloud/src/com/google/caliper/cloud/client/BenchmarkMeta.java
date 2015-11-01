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

package com.google.caliper.cloud.client;

import java.io.Serializable;
import java.util.List;

public final class BenchmarkMeta
    implements Serializable /* for GWT Serialization */ {
  private /*final*/ Long snapshotId;
  private /*final*/ Benchmark benchmark;
  private /*final*/ List<BenchmarkSnapshotMeta> snapshots;

  public BenchmarkMeta(Benchmark benchmark, List<BenchmarkSnapshotMeta> snapshots) {
    this(null, benchmark, snapshots);
  }
  
  public BenchmarkMeta(/*@Nullable*/ Long snapshotId, Benchmark benchmark,
      List<BenchmarkSnapshotMeta> snapshots) {
    this.benchmark = benchmark;
    this.snapshots = snapshots;
    this.snapshotId = snapshotId;
  }

  public Benchmark getBenchmark() {
    return benchmark;
  }

  public List<BenchmarkSnapshotMeta> getSnapshots() {
    return snapshots;
  }

  public Long getSnapshotId() {
    return snapshotId;
  }

  private BenchmarkMeta() {}
}
