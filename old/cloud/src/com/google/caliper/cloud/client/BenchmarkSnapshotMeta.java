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
import java.util.Comparator;

public final class BenchmarkSnapshotMeta
    implements Deletable, Serializable /* for GWT Serialization */ {
  public static final Comparator<BenchmarkSnapshotMeta> ORDER_BY_DATE =
      new Comparator<BenchmarkSnapshotMeta>() {
        public int compare(BenchmarkSnapshotMeta a, BenchmarkSnapshotMeta b) {
          if (a.created < b.created) {
            return -1;
          } else if (a.created > b.created) {
            return 1;
          }
          return 0;
        }
      };

  private /*final*/ long created;
  private /*final*/ long id;
  private /*final*/ String benchmarkOwner;  
  private /*final*/ String benchmarkName;  

  private boolean deleted;

  public BenchmarkSnapshotMeta(String benchmarkOwner, String benchmarkName,
      long created, long id) {
    this.created = created;
    this.id = id;
    this.benchmarkName = benchmarkName;
    this.benchmarkOwner = benchmarkOwner;
  }

  public boolean getDeleted() {
    return deleted;
  }

  public void setDeleted(boolean deleted) {
    this.deleted = deleted;
  }

  public long getCreated() {
    return created;
  }

  public long getId() {
    return id;
  }

  public String getBenchmarkOwner() {
    return benchmarkOwner;
  }

  public String getBenchmarkName() {
    return benchmarkName;
  }

  private BenchmarkSnapshotMeta() {} // for GWT Serialization
}
