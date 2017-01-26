/*
 * Copyright (C) 2013 Google Inc.
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

package com.google.caliper.model;

import static org.junit.Assert.assertEquals;

import com.google.caliper.json.GsonModule;
import com.google.common.collect.ImmutableMap;
import com.google.gson.Gson;
import dagger.Component;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/** Tests {@link Host} */
@RunWith(JUnit4.class)
public class HostTest {
  @Test
  public void hash() {
    int expected =
        PersistentHashing.getPersistentHashFunction()
            .newHasher()
            .putObject(ImmutableMap.of("a", "1", "b", "2"), StringMapFunnel.INSTANCE)
            .hash()
            .asInt();
    Host host = new Host.Builder().addProperty("a", "1").addProperty("b", "2").build();
    assertEquals(expected, host.hashCode());
  }

  @Test
  public void hash_afterJsonSerialization() {
    int expected =
        PersistentHashing.getPersistentHashFunction()
            .newHasher()
            .putObject(ImmutableMap.of("a", "1", "b", "2"), StringMapFunnel.INSTANCE)
            .hash()
            .asInt();
    Host host = new Host.Builder().addProperty("a", "1").addProperty("b", "2").build();
    Gson gson = DaggerHostTest_GsonComponent.create().gson();
    assertEquals(expected, gson.fromJson(gson.toJson(host), Host.class).hashCode());
  }

  @Component(modules = GsonModule.class)
  interface GsonComponent {
    Gson gson();
  }
}
