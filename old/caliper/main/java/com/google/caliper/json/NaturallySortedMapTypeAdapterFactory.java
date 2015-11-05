/*
 * Copyright (C) 2012 Google Inc.
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

package com.google.caliper.json;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Maps;
import com.google.gson.Gson;
import com.google.gson.TypeAdapter;
import com.google.gson.TypeAdapterFactory;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;

import java.io.IOException;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;

/**
 * Serializes and deserializes {@link SortedMap} instances using a {@link TreeMap} with natural
 * ordering as an intermediary.
 */
final class NaturallySortedMapTypeAdapterFactory implements TypeAdapterFactory {
  @SuppressWarnings("rawtypes")
  private static final ImmutableSet<Class<? extends SortedMap>> CLASSES =
      ImmutableSet.of(SortedMap.class, TreeMap.class);

  @SuppressWarnings("unchecked")
  @Override public <T> TypeAdapter<T> create(Gson gson, TypeToken<T> typeToken) {
    Type type = typeToken.getType();
    if (!CLASSES.contains(typeToken.getRawType())
        || !(type instanceof ParameterizedType)) {
      return null;
    }

    com.google.common.reflect.TypeToken<SortedMap<?, ?>> betterToken =
        (com.google.common.reflect.TypeToken<SortedMap<?, ?>>)
            com.google.common.reflect.TypeToken.of(typeToken.getType());
    final TypeAdapter<Map<?, ?>> mapAdapter =
        (TypeAdapter<Map<?, ?>>) gson.getAdapter(
            TypeToken.get(betterToken.getSupertype(Map.class).getType()));
    return new TypeAdapter<T>() {
      @Override public void write(JsonWriter out, T value) throws IOException {
        TreeMap<?, ?> treeMap = Maps.newTreeMap((SortedMap<?, ?>) value);
        mapAdapter.write(out, treeMap);
      }

      @SuppressWarnings("rawtypes")
      @Override public T read(JsonReader in) throws IOException {
        TreeMap treeMap = Maps.newTreeMap();
        treeMap.putAll(mapAdapter.read(in));
        return (T) treeMap;
      }
    };
  }
}
