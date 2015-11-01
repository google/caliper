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

import com.google.appengine.api.datastore.Blob;
import com.google.appengine.api.datastore.DatastoreService;
import com.google.appengine.api.datastore.DatastoreServiceFactory;
import com.google.appengine.api.datastore.Entity;
import com.google.appengine.api.datastore.EntityNotFoundException;
import com.google.appengine.api.datastore.FetchOptions;
import com.google.appengine.api.datastore.KeyFactory;
import com.google.appengine.api.datastore.Query;
import com.google.caliper.cloud.client.Benchmark;
import com.google.common.collect.Lists;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.List;

public final class BenchmarkSnapshotStore {
  private static final int MAX_RESULTS = 30;
  private final DatastoreService datastoreService = DatastoreServiceFactory.getDatastoreService();

  private BenchmarkSnapshot entityToBenchmark(Entity entity) {
    InputStream in = new ByteArrayInputStream(((Blob) entity.getProperty("xml")).getBytes());
    return new BenchmarkSnapshot(Xml.benchmarkFromXml(in), (Long) entity.getProperty("created"),
        entity.getKey().getId());
  }

  public Entity getBenchmarkSnapshot(long id) {
    try {
      return datastoreService.get(KeyFactory.createKey("benchmarkSnapshot", id));
    } catch (EntityNotFoundException e) {
      throw new RuntimeException(e);
    }
  }

  public void setDeleted(Entity benchmarkSnapshot, boolean deleted) {
    benchmarkSnapshot.setProperty("deleted", deleted);
    datastoreService.put(benchmarkSnapshot);
  }

  public List<BenchmarkSnapshot> fetchBenchmarkSnapshots(String benchmarkOwner,
      String benchmarkName) {
    Query query = new Query("benchmarkSnapshot");
    query.addFilter("emailAddress", Query.FilterOperator.EQUAL, benchmarkOwner);
    query.addFilter("benchmarkName", Query.FilterOperator.EQUAL, benchmarkName);
    query.addFilter("deleted", Query.FilterOperator.EQUAL, false);
    Iterable<Entity> benchmarkEntities = datastoreService.prepare(query).asIterable(
        FetchOptions.Builder.withLimit(MAX_RESULTS).prefetchSize(MAX_RESULTS));

    List<BenchmarkSnapshot> benchmarkSnapshots = Lists.newArrayList();
    for (Entity benchmarkEntity : benchmarkEntities) {
      benchmarkSnapshots.add(entityToBenchmark(benchmarkEntity));
    }

    return benchmarkSnapshots;
  }

  public long createSnapshot(Benchmark benchmark) {
    Entity snapshot = new Entity("benchmarkSnapshot");
    snapshot.setProperty("emailAddress", benchmark.getOwner());
    snapshot.setProperty("benchmarkName", benchmark.getName());
    snapshot.setProperty("created", System.currentTimeMillis());
    snapshot.setProperty("deleted", false);

    ByteArrayOutputStream bytes = new ByteArrayOutputStream();
    Xml.benchmarkToXml(benchmark, bytes);
    snapshot.setProperty("xml", new Blob(bytes.toByteArray()));

    datastoreService.put(snapshot);

    return snapshot.getKey().getId();
  }
}
