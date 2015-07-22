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

import com.google.gson.ExclusionStrategy;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.TypeAdapterFactory;
import com.google.gson.internal.bind.TypeAdapters;
import dagger.Module;
import dagger.Provides;
import dagger.Provides.Type;
import org.joda.time.Instant;
import java.util.Set;

/**
 * Binds a {@link Gson} instance suitable for serializing and deserializing Caliper
 * {@linkplain com.google.caliper.model model} objects.
 */
@Module
public final class GsonModule {

  @Provides(type = Type.SET)
  static TypeAdapterFactory provideImmutableListTypeAdapterFactory() {
    return new ImmutableListTypeAdatperFactory();
  }

  @Provides(type = Type.SET)
  static TypeAdapterFactory provideImmutableMapTypeAdapterFactory() {
    return new ImmutableMapTypeAdapterFactory();
  }

  @Provides(type = Type.SET)
  static TypeAdapterFactory provideNaturallySortedMapTypeAdapterFactory() {
    return new NaturallySortedMapTypeAdapterFactory();
  }

  @Provides(type = Type.SET)
  static TypeAdapterFactory provideImmutableMultimapTypeAdapterFactory() {
    return new ImmutableMultimapTypeAdapterFactory();
  }

  @Provides
  static ExclusionStrategy provideAnnotationExclusionStrategy() {
    return new AnnotationExclusionStrategy();
  }

  @Provides(type = Type.SET)
  static TypeAdapterFactory provideTypeAdapterFactoryForInstant(
      InstantTypeAdapter typeAdapter) {
    return TypeAdapters.newFactory(Instant.class, typeAdapter);
  }

  @Provides static InstantTypeAdapter provideInstantTypeAdapter() {
    return new InstantTypeAdapter();
  }

  @Provides static Gson provideGson(Set<TypeAdapterFactory> typeAdapterFactories,
      ExclusionStrategy exclusionStrategy) {
    GsonBuilder gsonBuilder = new GsonBuilder().setExclusionStrategies(exclusionStrategy);
    for (TypeAdapterFactory typeAdapterFactory : typeAdapterFactories) {
      gsonBuilder.registerTypeAdapterFactory(typeAdapterFactory);
    }
    return gsonBuilder.create();
  }
}
