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

import java.security.AccessControlException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import com.google.appengine.api.datastore.Entity;
import com.google.appengine.api.users.UserService;
import com.google.appengine.api.users.UserServiceFactory;
import com.google.caliper.cloud.client.Benchmark;
import com.google.caliper.cloud.client.BenchmarkMeta;
import com.google.caliper.cloud.client.BenchmarkService;
import com.google.caliper.cloud.client.BenchmarkSnapshotMeta;
import com.google.caliper.cloud.client.RunMeta;
import com.google.common.base.Function;
import com.google.common.collect.Lists;
import com.google.gwt.user.server.rpc.RemoteServiceServlet;

/**
 * The server side implementation of the RPC service.
 */
@SuppressWarnings("serial")
public class BenchmarkServiceImpl extends RemoteServiceServlet
    implements BenchmarkService {

  private static final Logger logger = Logger.getLogger(BenchmarkServiceImpl.class.getName());

  public Long createSnapshot(Benchmark benchmark) {
    return new BenchmarkSnapshotStore().createSnapshot(benchmark);
  }

  public List<String> fetchBenchmarkNames(String benchmarkOwner) {
    Iterable<Entity> benchmarkEntities = new BenchmarkStore().getBenchmarksByOwner(benchmarkOwner);
    List<String> benchmarkNames = new ArrayList<String>();
    for (Entity benchmarkEntity : benchmarkEntities) {
      String benchmarkName = (String) benchmarkEntity.getProperty("benchmarkName");
      if (benchmarkName != null) {
        benchmarkNames.add(benchmarkName);
      }
    }
    return benchmarkNames;
  }

  public BenchmarkMeta fetchBenchmark(String benchmarkOwner, String benchmarkName,
      Long snapshotId) {
    List<BenchmarkSnapshot> snapshots =
        new BenchmarkSnapshotStore().fetchBenchmarkSnapshots(benchmarkOwner, benchmarkName);
    Benchmark benchmark = null;
    if (snapshotId != null) {
      for (BenchmarkSnapshot snapshot: snapshots) {
        if (snapshotId.equals(snapshot.getMetadata().getId())) {
          benchmark = snapshot.getBenchmark();
          break;
        }
      }
      if (benchmark == null) {
        return null; // invalid snapshot id
      }
    } else {
      // TODO collate environments separately to avoid redundant wire traffic
      List<RunMeta> runs = new RunStore().lookupRuns(benchmarkOwner, benchmarkName);
  
      BenchmarkStore benchmarkStore = new BenchmarkStore();
      Entity benchmarkEntity = benchmarkStore.getBenchmark(benchmarkOwner, benchmarkName);
      List<String> rVariables = benchmarkStore.getRVariables(benchmarkEntity);
      String cVariable = benchmarkStore.getCVariable(benchmarkEntity);
      Map<String, Map<String, Boolean>> variableValuesShown =
          benchmarkStore.getVariableValuesShown(benchmarkEntity);
  
      logger.info("Returning " + runs.size() + " runs for " +
          benchmarkOwner + "/" + benchmarkName);
      benchmark = new Benchmark(benchmarkOwner, benchmarkName, runs, rVariables, cVariable,
          variableValuesShown);
    }
    return new BenchmarkMeta(
        benchmark,
        Lists.newArrayList(Lists.transform(snapshots,
            new Function<BenchmarkSnapshot, BenchmarkSnapshotMeta>() {
              @Override public BenchmarkSnapshotMeta apply(BenchmarkSnapshot snapshot) {
                return snapshot.getMetadata();
              }
            })));
  }

  public void nameRun(long id, String name) {
    RunStore runStore = new RunStore();
    Entity run = runStore.getRun(id);
    checkEditPermission(run);
    runStore.nameRun(run, name);
  }

  public void nameEnvironment(long id, String name) {
    EnvironmentStore environmentStore = new EnvironmentStore();
    Entity environment = environmentStore.getEnvironment(id);
    checkEditPermission(environment);
    new EnvironmentStore().nameEnvironment(environment, name);
  }

  public void setRunDeleted(long id, boolean deleted) {
    RunStore runStore = new RunStore();
    Entity run = runStore.getRun(id);
    checkEditPermission(run);
    runStore.setDeleted(run, deleted);
  }

  public void setSnapshotDeleted(long id, boolean deleted) {
    BenchmarkSnapshotStore snapshotStore = new BenchmarkSnapshotStore();
    Entity snapshot = snapshotStore.getBenchmarkSnapshot(id);
    checkEditPermission(snapshot);
    snapshotStore.setDeleted(snapshot, deleted);
  }

  public void reorderVariables(String benchmarkOwner, String benchmarkName,
      List<String> rVariables, String cVariable) {
    BenchmarkStore benchmarkStore = new BenchmarkStore();
    Entity benchmark = benchmarkStore.getBenchmark(benchmarkOwner, benchmarkName);
    checkEditPermission(benchmark);
    benchmarkStore.reorderVariables(benchmark, rVariables, cVariable);
    logger.info("Saved reordered variables: " + rVariables + ", " + cVariable);
  }


  public void setVariableValuesShown(String benchmarkOwner, String benchmarkName,
      Map<String, Map<String, Boolean>> variableValuesShown) {
    BenchmarkStore benchmarkStore = new BenchmarkStore();
    Entity benchmark = benchmarkStore.getBenchmark(benchmarkOwner, benchmarkName);
    checkEditPermission(benchmark);
    benchmarkStore.setVariableValuesShown(benchmark, variableValuesShown);
  }

  private void checkEditPermission(Entity entity) {
    UserService userService = UserServiceFactory.getUserService();
    if (!userService.isUserLoggedIn()) {
      throw new AccessControlException("Not logged in");
    }

    String currentUserEmail = userService.getCurrentUser().getEmail();
    String runOwner = (String) entity.getProperty("emailAddress");
    if (!currentUserEmail.equals(runOwner)) {
      throw new AccessControlException("Cannot edit " + runOwner + " as " + currentUserEmail);
    }
  }
}
