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

import com.google.caliper.Environment;

import java.io.Serializable;
import java.util.Comparator;

public class EnvironmentMeta
    implements Nameable, Serializable /* for GWT Serialization */ {

  public static final Comparator<? super EnvironmentMeta> ORDER_BY_DATE =
      new Comparator<EnvironmentMeta>() {
        public int compare(EnvironmentMeta a, EnvironmentMeta b) {
          long aCreated = a.getCreated();
          long bCreated = b.getCreated();
          if (aCreated == bCreated) {
            return 0;
          } else if (aCreated < bCreated) {
            return -1;
          } else {
            return 1;
          }
        }
      };

  private /*final*/ long id;
  private /*final*/ Environment environment;

  private /*final*/ long created;
  private String name;

  public EnvironmentMeta(long id, Environment environment, String name, long created) {
    this.id = id;
    this.environment = environment;
    this.created = created;
    this.name = name;
  }

  public long getId() {
    return id;
  }

  public Environment getEnvironment() {
    return environment;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public long getCreated() {
    return created;
  }
  
  private EnvironmentMeta() {} // for GWT Serialization
}
