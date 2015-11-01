/**
 * Copyright (C) 2009 Google Inc.
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
import com.google.appengine.api.datastore.Key;
import com.google.appengine.api.datastore.KeyFactory;
import com.google.appengine.api.datastore.Query;
import com.google.caliper.Json;
import com.google.caliper.Run;
import com.google.caliper.Xml;
import com.google.caliper.cloud.client.EnvironmentMeta;
import com.google.caliper.cloud.client.RunMeta;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

/**
 * Table: "run"
 * emailAddress: String
 * benchmarkName: String, usually a class name
 * name: String (optional)
 * deleted: boolean
 * environmentKey: Key
 * executedTimestamp: Date
 * xml: byte[] containing {@code <run/>}
 */
public final class RunStore {

  private static final int MAX_RESULTS = 26;
  private final DatastoreService datastoreService = DatastoreServiceFactory.getDatastoreService();
  private final EnvironmentStore environmentStore = new EnvironmentStore();

  private Collection<Entity> getRuns(List<Long> longKeys) {
    List<Key> keys = new ArrayList<Key>();
    for (long longKey : longKeys) {
      keys.add(KeyFactory.createKey("run", longKey));
    }
    return datastoreService.get(keys).values();
  }

  public List<RunMeta> getRunMetas(List<Long> keys) {
    return entitiesToRunMetas(getRuns(keys), false);
  }

  public Entity getRun(long id) {
    try {
      return datastoreService.get(KeyFactory.createKey("run", id));
    } catch (EntityNotFoundException e) {
      throw new RuntimeException(e.getMessage());
    }
  }

  public void nameRun(Entity run, String name) {
    run.setProperty("name", name);
    datastoreService.put(run);
  }

  public void setDeleted(Entity run, boolean deleted) {
    run.setProperty("deleted", deleted);
    datastoreService.put(run);
  }
  
  public void createRun(Run run, Entity environmentEntity, String emailAddress) {
    DatastoreService datastoreService = DatastoreServiceFactory.getDatastoreService();
    Entity entity = runToEntity(run, environmentEntity, emailAddress);
    Entity benchmark = new BenchmarkStore().getBenchmark(emailAddress, run.getBenchmarkName());
    datastoreService.put(benchmark);
    datastoreService.put(entity);
  }

  public List<RunMeta> lookupRuns(String benchmarkOwner, String benchmarkName) {
    // fetch the runs from the DB
    Query query = new Query("run");
    query.addFilter("emailAddress", Query.FilterOperator.EQUAL, benchmarkOwner);
    query.addFilter("benchmarkName", Query.FilterOperator.EQUAL, benchmarkName);
    Iterable<Entity> entities = datastoreService.prepare(query).asIterable(
        FetchOptions.Builder.withLimit(MAX_RESULTS).prefetchSize(MAX_RESULTS));

    return entitiesToRunMetas(entities, true);
  }

  private List<RunMeta> entitiesToRunMetas(Iterable<Entity> entities, boolean filterDeleted) {
    List<RunMeta> runs = new ArrayList<RunMeta>();
    Set<Key> environmentKeys = new HashSet<Key>();
    for (Entity entity : entities) {
      Run run = entityToRun(entity);

      if (filterDeleted) {
        /*
         * TODO: fix old data in the datastore to include a 'deleted' attribute, so
         *       we can filter the query rather than after the fact
         */
        Boolean deleted = (Boolean) entity.getProperty("deleted");
        if (deleted != null && deleted) {
          continue;
        }
      }

      Key environmentKey = (Key) entity.getProperty("environmentKey");
      if (environmentKey != null) {
        environmentKeys.add(environmentKey);
      }
      String environmentKeyString =
          environmentKey == null ? null : KeyFactory.keyToString(environmentKey);

      long id = entity.getKey().getId();
      String name = (String) entity.getProperty("name");

      runs.add(new RunMeta(id, run, name, environmentKeyString));
    }

    Map<Key, EnvironmentMeta> environments =
        environmentStore.lookupEnvironments(environmentKeys);

    Collections.sort(runs, RunMeta.ORDER_BY_DATE);

    int runCount = 0;
    for (RunMeta run : runs) {
      if (run.getName() == null) {
        run.setName(nameRun(runCount));
      }
      run.setStyle(runCount);
      if (run.getEnvironmentKey() != null) {
        run.setEnvironmentMeta(environments.get(KeyFactory.stringToKey(run.getEnvironmentKey())));
      }
      runCount++;
    }

    return runs;
  }

  private String nameRun(int index) {
    // TODO: this won't really work for more than 26 runs...
    return String.valueOf((char) ('A' + index));
  }

  /**
   * Encodes a run as an app engine entity for long term persistence.
   */
  private Entity runToEntity(Run run, Entity environmentEntity, String emailAddress) {
    Entity result = new Entity("run");
    result.setProperty("executedTimestamp", run.getExecutedTimestamp());
    result.setProperty("emailAddress", emailAddress);
    result.setProperty("benchmarkName", run.getBenchmarkName());
    result.setProperty("deleted", false);
    result.setProperty("environmentKey", environmentEntity.getKey());
    // indicates that the blob has been zipped, so that we know to unzip it. This is for
    // backwards compatibility with blobs previously added that were not zipped.
    result.setProperty("zipped", true);
    result.setProperty("json", new Blob(gzip(Json.getGsonInstance().toJson(run).getBytes())));

    return result;
  }

  /**
   * Encodes a run as an app engine entity for long term persistence.
   */
  private Run entityToRun(Entity entity) {
    if (entity.hasProperty("xml")) {
      InputStream in;
      Boolean zipped = (Boolean) entity.getProperty("zipped");
      if (zipped != null && zipped) {
        in = new ByteArrayInputStream(ungzip(((Blob) entity.getProperty("xml")).getBytes()));
      } else {
        in = new ByteArrayInputStream(((Blob) entity.getProperty("xml")).getBytes());
      }
      return Xml.runFromXml(in);
    } else if (entity.hasProperty("json")) {
      // json is assumed to be zipped
      byte[] bytes = ungzip(((Blob) entity.getProperty("json")).getBytes());
      Reader reader = new InputStreamReader(new ByteArrayInputStream(bytes));
      return Json.getGsonInstance().fromJson(reader, Run.class);
    } else {
      throw new RuntimeException("Run entity has neither a json nor an xml field");
    }
  }

  public static byte[] gzip(byte[] unzippedBytes) {
    ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
    try {
      GZIPOutputStream gzipOutputStream = new GZIPOutputStream(outputStream);
      gzipOutputStream.write(unzippedBytes);
      gzipOutputStream.close();
    } catch (IOException e) {
      throw new RuntimeException("failed to zip byte array", e);
    }
    return outputStream.toByteArray();
  }

  public static byte[] ungzip(byte[] zippedBytes) {
    ByteArrayOutputStream out = new ByteArrayOutputStream();
    try {
      byte[] b = new byte[4096];
      InputStream gzipInputStream = new GZIPInputStream(new ByteArrayInputStream(zippedBytes));
      int bytesRead;
      while ((bytesRead = gzipInputStream.read(b)) != -1) {
        out.write(b, 0, bytesRead);
      }
    } catch (IOException e) {
      throw new RuntimeException("failed to unzip byte array", e);
    }
    return out.toByteArray();
  }

}
