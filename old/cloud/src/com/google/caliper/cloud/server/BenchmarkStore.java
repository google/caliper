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

import com.google.appengine.api.datastore.DatastoreService;
import com.google.appengine.api.datastore.DatastoreServiceFactory;
import com.google.appengine.api.datastore.Entity;
import com.google.appengine.api.datastore.Query;
import com.google.common.base.Joiner;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class BenchmarkStore {

  private final DatastoreService datastoreService = DatastoreServiceFactory.getDatastoreService();

  public Iterable<Entity> getBenchmarksByOwner(String benchmarkOwner) {
    Query query = new Query("benchmark");
    query.addFilter("emailAddress", Query.FilterOperator.EQUAL, benchmarkOwner);
    return datastoreService.prepare(query).asIterable();
  }

  /**
   * Returns the benchmark with the given owner and name.
   */
  public Entity getBenchmark(String benchmarkOwner, String benchmarkName) {
    Query query = new Query("benchmark");
    query.addFilter("emailAddress", Query.FilterOperator.EQUAL, benchmarkOwner);
    query.addFilter("benchmarkName", Query.FilterOperator.EQUAL, benchmarkName);
    Entity benchmark = datastoreService.prepare(query).asSingleEntity();

    if (benchmark == null) {
      benchmark = new Entity("benchmark");
      benchmark.setProperty("emailAddress", benchmarkOwner);
      benchmark.setProperty("benchmarkName", benchmarkName);
    }

    return benchmark;
  }

  public void reorderVariables(Entity benchmark, List<String> rVariables, String cVariable) {
    benchmark.setProperty("rVariables", Joiner.on(",").join(rVariables));
    if (cVariable != null) {
      benchmark.setProperty("cVariable", cVariable);
    } else {
      benchmark.removeProperty("cVariable");
    }
    datastoreService.put(benchmark);
  }

  public List<String> getRVariables(Entity entity) {
    String rVariables = (String) entity.getProperty("rVariables");
    List<String> result = new ArrayList<String>();
    if (rVariables != null && !rVariables.isEmpty()) {
      result.addAll(Arrays.asList(rVariables.split("\\,")));
    }
    return result;
  }

  public String getCVariable(Entity entity) {
    return (String) entity.getProperty("cVariable");
  }

  public Map<String, Map<String, Boolean>> getVariableValuesShown(Entity entity) {
    String variableValuesShownJson = (String) entity.getProperty("variableValuesShown");
    if (variableValuesShownJson == null) {
      return new HashMap<String, Map<String, Boolean>>();
    }
    return new Gson().fromJson(variableValuesShownJson,
        new TypeToken<Map<String, Map<String, Boolean>>>() {}.getType());
  }

  public void setVariableValuesShown(Entity benchmark,
      Map<String, Map<String, Boolean>> variableValuesShown) {
    String variableValuesShownJson = new Gson().toJson(variableValuesShown);
    benchmark.setProperty("variableValuesShown", variableValuesShownJson);
    datastoreService.put(benchmark);
  }
}