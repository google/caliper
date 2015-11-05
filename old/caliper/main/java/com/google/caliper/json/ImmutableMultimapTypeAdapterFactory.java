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

import com.google.common.collect.ImmutableListMultimap;
import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.ImmutableSetMultimap;
import com.google.common.collect.ListMultimap;
import com.google.common.collect.SetMultimap;
import com.google.common.reflect.TypeParameter;
import com.google.common.reflect.TypeToken;
import com.google.gson.Gson;
import com.google.gson.TypeAdapter;
import com.google.gson.TypeAdapterFactory;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;

import java.io.IOException;
import java.lang.reflect.ParameterizedType;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

/**
 * Serializes and deserializes {@link ImmutableMultimap} instances using maps of collections as
 * intermediaries.
 */
final class ImmutableMultimapTypeAdapterFactory implements TypeAdapterFactory {
  private static <K, V> TypeToken<Map<K, List<V>>> getMapOfListsToken(
      TypeToken<ListMultimap<K, V>> from) {
    ParameterizedType rawType = (ParameterizedType) from.getSupertype(ListMultimap.class).getType();
    @SuppressWarnings("unchecked") // key type is K
    TypeToken<K> keyType = (TypeToken<K>) TypeToken.of(rawType.getActualTypeArguments()[0]);
    @SuppressWarnings("unchecked") // value type is V
    TypeToken<V> valueType = (TypeToken<V>) TypeToken.of(rawType.getActualTypeArguments()[1]);
    return new TypeToken<Map<K, List<V>>>() {}
        .where(new TypeParameter<K>() {}, keyType)
        .where(new TypeParameter<V>() {}, valueType);
  }

  private static <K, V> TypeToken<Map<K, Set<V>>> getMapOfSetsToken(
      TypeToken<SetMultimap<K, V>> from) {
    ParameterizedType rawType = (ParameterizedType) from.getSupertype(SetMultimap.class).getType();
    @SuppressWarnings("unchecked") // key type is K
    TypeToken<K> keyType = (TypeToken<K>) TypeToken.of(rawType.getActualTypeArguments()[0]);
    @SuppressWarnings("unchecked") // value type is V
    TypeToken<V> valueType = (TypeToken<V>) TypeToken.of(rawType.getActualTypeArguments()[1]);
    return new TypeToken<Map<K, Set<V>>>() {}
        .where(new TypeParameter<K>() {}, keyType)
        .where(new TypeParameter<V>() {}, valueType);
  }

  @Override
  @SuppressWarnings({"unchecked", "rawtypes"})
  public <T> TypeAdapter<T> create(Gson gson, com.google.gson.reflect.TypeToken<T> typeToken) {
    if (ImmutableListMultimap.class.isAssignableFrom(typeToken.getRawType())) {
      TypeToken<Map<?, List<?>>> mapToken =
          getMapOfListsToken((TypeToken) TypeToken.of(typeToken.getType()));
      final TypeAdapter<Map<?, List<?>>> adapter =
          (TypeAdapter<Map<?, List<?>>>) gson.getAdapter(
              com.google.gson.reflect.TypeToken.get(mapToken.getType()));
      return new TypeAdapter<T>() {
        @Override public void write(JsonWriter out, T value) throws IOException {
          ImmutableListMultimap<?, ?> multimap = (ImmutableListMultimap<?, ?>) value;
          adapter.write(out, (Map) multimap.asMap());
        }

        @Override public T read(JsonReader in) throws IOException {
          Map<?, List<?>> value = adapter.read(in);
          ImmutableListMultimap.Builder builder = ImmutableListMultimap.builder();
          for (Entry<?, List<?>> entry : value.entrySet()) {
            builder.putAll(entry.getKey(), entry.getValue());
          }
          return (T) builder.build();
        }
      };
    } else if (ImmutableSetMultimap.class.isAssignableFrom(typeToken.getRawType())) {
      TypeToken<Map<?, Set<?>>> mapToken =
          getMapOfSetsToken((TypeToken) TypeToken.of(typeToken.getType()));
      final TypeAdapter<Map<?, Set<?>>> adapter =
          (TypeAdapter<Map<?, Set<?>>>) gson.getAdapter(
              com.google.gson.reflect.TypeToken.get(mapToken.getType()));
      return new TypeAdapter<T>() {
        @Override public void write(JsonWriter out, T value) throws IOException {
          ImmutableSetMultimap<?, ?> multimap = (ImmutableSetMultimap<?, ?>) value;
          adapter.write(out, (Map) multimap.asMap());
        }

        @Override public T read(JsonReader in) throws IOException {
          Map<?, Set<?>> value = adapter.read(in);
          ImmutableSetMultimap.Builder builder = ImmutableSetMultimap.builder();
          for (Entry<?, Set<?>> entry : value.entrySet()) {
            builder.putAll(entry.getKey(), entry.getValue());
          }
          return (T) builder.build();
        }
      };
    } else {
      return null;
    }
  }
}
