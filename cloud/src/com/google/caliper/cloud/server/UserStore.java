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

import com.google.appengine.api.datastore.DatastoreService;
import com.google.appengine.api.datastore.DatastoreServiceFactory;
import com.google.appengine.api.datastore.Entity;
import com.google.appengine.api.datastore.Query;
import java.util.UUID;
import java.util.logging.Logger;

/**
 * Table "user"
 * emailAddress: String
 * apiKey: String
 *
 */
public class UserStore {

  private static final Logger logger = Logger.getLogger(UserStore.class.getName());
  private final DatastoreService datastoreService = DatastoreServiceFactory.getDatastoreService();

  /**
   * Returns the API key for the user with the given email address. If no API
   * key exists, a new one will be created.
   */
  public String getOrCreateApiKey(String emailAddress) {
    Query query = new Query("user");
    query.addFilter("emailAddress", Query.FilterOperator.EQUAL, emailAddress);
    Entity entity = datastoreService.prepare(query).asSingleEntity();

    if (entity == null) {
      entity = new Entity("user");
      String apiKey = UUID.randomUUID().toString();
      entity.setProperty("emailAddress", emailAddress);
      entity.setProperty("apiKey", apiKey);
      logger.info("Creating API key " + apiKey + " for " + emailAddress);
      datastoreService.put(entity);
    }

    return (String) entity.getProperty("apiKey");
  }

  /**
   * Returns the email address associated with the given API key.
   */
  public String lookupEmailAddress(String apiKey) {
    Query query = new Query("user");
    query.addFilter("apiKey", Query.FilterOperator.EQUAL, apiKey);
    Entity entity = datastoreService.prepare(query).asSingleEntity();
    if (entity == null) {
      throw new IllegalStateException("No email address found for " + apiKey);
    }
    return (String) entity.getProperty("emailAddress");
  }
}
