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
import com.google.appengine.api.datastore.Key;
import com.google.appengine.api.datastore.KeyFactory;
import com.google.appengine.api.datastore.Query;
import com.google.caliper.Environment;
import com.google.caliper.Json;
import com.google.caliper.Xml;
import com.google.caliper.cloud.client.EnvironmentMeta;
import com.google.gson.Gson;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Table: "environment"
 * emailAddress: String
 * name: String (optional)
 * created: long
 * xml: byte[] containing {@code <environment/>}
 */
public final class EnvironmentStore {
  private final DatastoreService datastoreService = DatastoreServiceFactory.getDatastoreService();

  public Entity getEnvironment(long id) {
    try {
      return datastoreService.get(KeyFactory.createKey("environment", id));
    } catch (EntityNotFoundException e) {
      throw new RuntimeException(e.getMessage());
    }
  }

  public void nameEnvironment(Entity environment, String name) {
    environment.setProperty("name", name);
    datastoreService.put(environment);
  }

  /**
   * Returns an entity representing {@code environment}, using an existing entity if possible, or
   * creating a new one if not.
   */
  public Entity getOrCreateEnvironment(Environment environment, String emailAddress) {
    DatastoreService datastoreService = DatastoreServiceFactory.getDatastoreService();

    Query query = new Query("environment");
    query.addFilter("emailAddress", Query.FilterOperator.EQUAL, emailAddress);
    Iterable<Entity> entities = datastoreService.prepare(query).asIterable();

    for (Entity entity : entities) {
      Environment entityEnvironment = entityToEnvironment(entity);
      if (entityEnvironment.getProperties().equals(environment.getProperties())) {
        return entity;
      }
    }

    Entity entity = environmentToEntity(environment, emailAddress);
    datastoreService.put(entity);
    return entity;
  }

  /**
   * Gets all environments corresponding to the keys in {@code environmentIds}.
   */
  public Map<Key, EnvironmentMeta> lookupEnvironments(Iterable<Key> environmentIds) {
    Map<Key, EnvironmentMeta> environmentMap = new HashMap<Key, EnvironmentMeta>();
    for (Map.Entry<Key, Entity> entry : datastoreService.get(environmentIds).entrySet()) {
        Environment environment = entityToEnvironment(entry.getValue());
        long id = entry.getKey().getId();
        String name = (String) entry.getValue().getProperty("name");
        long created = (Long) entry.getValue().getProperty("created");
        environmentMap.put(entry.getKey(), new EnvironmentMeta(id, environment, name, created));
    }

    int namedEnvironmentCount = 0;
    List<EnvironmentMeta> environments = new ArrayList<EnvironmentMeta>(environmentMap.values());
    Collections.sort(environments, EnvironmentMeta.ORDER_BY_DATE);
    for (EnvironmentMeta environment : environments) {
      if (environment.getName() == null) {
        environment.setName(nameEnvironment(namedEnvironmentCount));
      }
      namedEnvironmentCount++;
    }
    return environmentMap;
  }

  private Entity environmentToEntity(Environment environment, String emailAddress) {
    // hack - stolen from RunStore
    System.setProperty("javax.xml.transform.TransformerFactory",
        "com.sun.org.apache.xalan.internal.xsltc.trax.TransformerFactoryImpl");

    Entity result = new Entity("environment");
    result.setProperty("emailAddress", emailAddress);
    result.setProperty("json", new Blob(new Gson().toJson(environment).getBytes()));
    result.setProperty("created", System.currentTimeMillis());
    return result;
  }

  private Environment entityToEnvironment(Entity entity) {
    if (entity.getProperty("xml") != null) {
      InputStream in = new ByteArrayInputStream(((Blob) entity.getProperty("xml")).getBytes());
      return Xml.environmentFromXml(in);
    } else if (entity.getProperty("json") != null) {
      byte[] bytes = ((Blob) entity.getProperty("json")).getBytes();
      Reader reader = new InputStreamReader(new ByteArrayInputStream(bytes));
      return Json.getGsonInstance().fromJson(reader, Environment.class);
    } else {
      throw new RuntimeException("environment entity has neither a json nor an xml field");
    }
  }

  private String nameEnvironment(int index) {
    // TODO: this won't really work for more than 26 runs...
    return String.valueOf((char) ('A' + index));
  }
}
