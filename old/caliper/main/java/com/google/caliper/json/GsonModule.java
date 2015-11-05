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

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.PARAMETER;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import com.google.gson.ExclusionStrategy;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.TypeAdapterFactory;
import com.google.gson.internal.bind.TypeAdapters;
import com.google.inject.AbstractModule;
import com.google.inject.BindingAnnotation;
import com.google.inject.Key;
import com.google.inject.Provides;
import com.google.inject.multibindings.Multibinder;

import org.joda.time.Instant;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;
import java.util.Set;

/**
 * Binds a {@link Gson} instance suitable for serializing and deserializing Caliper
 * {@linkplain com.google.caliper.model model} objects.
 */
public final class GsonModule extends AbstractModule {
  @Override protected void configure() {
    Multibinder<TypeAdapterFactory> typeAdapterFactoryMultibinder =
        Multibinder.newSetBinder(binder(), TypeAdapterFactory.class);
    typeAdapterFactoryMultibinder.addBinding().to(ImmutableListTypeAdatperFactory.class);
    typeAdapterFactoryMultibinder.addBinding().to(ImmutableMapTypeAdapterFactory.class);
    typeAdapterFactoryMultibinder.addBinding().to(NaturallySortedMapTypeAdapterFactory.class);
    typeAdapterFactoryMultibinder.addBinding()
        .to(Key.get(TypeAdapterFactory.class, ForInstant.class));
    typeAdapterFactoryMultibinder.addBinding().to(ImmutableMultimapTypeAdapterFactory.class);
    bind(ExclusionStrategy.class).to(AnnotationExclusionStrategy.class);
  }

  @Retention(RUNTIME)
  @Target({FIELD, PARAMETER, METHOD})
  @BindingAnnotation
  private @interface ForInstant {}

  @Provides @ForInstant TypeAdapterFactory provideTypeAdapterFactoryForInstant(InstantTypeAdapter typeAdapter) {
    return TypeAdapters.newFactory(Instant.class, typeAdapter);
  }

  @Provides Gson provideGson(Set<TypeAdapterFactory> typeAdapterFactories,
      ExclusionStrategy exclusionStrategy) {
    GsonBuilder gsonBuilder = new GsonBuilder().setExclusionStrategies(exclusionStrategy);
    for (TypeAdapterFactory typeAdapterFactory : typeAdapterFactories) {
      gsonBuilder.registerTypeAdapterFactory(typeAdapterFactory);
    }
    return gsonBuilder.create();
  }
}
