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

package com.google.caliper.runner;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.inject.AbstractModule;
import com.google.inject.Key;
import com.google.inject.Module;
import com.google.inject.OutOfScopeException;
import com.google.inject.Provider;
import com.google.inject.ProvisionException;
import com.google.inject.Scope;

import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;

/**
 * Implementation and utilty methods for managing the {@link TrialScoped} guice scope.
 *
 * <p>To use the TrialScope you must
 * <ul>
 *   <li>Install {@link #module()} to provide the binding annotation.
 *   <li>Construct a {@link TrialContext} via {@link #makeContext} and invoke
 *      {@link TrialContext#call} on it.
 * </ul>
 *
 * <p>Every guice interaction within the callable provided to {@link TrialContext#call} will have
 * access to the TrialScope.
 */
class TrialScopes {
  private static final ThreadLocal<TrialContext> contextLocal = new ThreadLocal<TrialContext>();

  /** The module that implements the scope. */
  static Module module() {
    return INTERNAL;
  }

  /**
   * Encapsulates the seed data for the trial scope.
   */
  static final class TrialContext {
    private final Cache<Key<?>, Object> contextMap = CacheBuilder.newBuilder()
        .concurrencyLevel(1)  // We shouldn't have more than one writer
        .build();

    private TrialContext(UUID trialId, int trialNumber, Experiment experiment) {
      contextMap.put(Key.get(UUID.class, TrialId.class), trialId);
      contextMap.put(Key.get(Integer.class, TrialNumber.class), trialNumber);
      contextMap.put(Key.get(Experiment.class), experiment);
    }

    <T> T call(final Callable<T> callable) throws Exception {
      // TODO(lukes): allow nesting scopes?  maybe only if it is the same scope?
      // seems reasonable since it would allow callbacks to be TrialScoped.
      if (contextLocal.get() != null) {
        throw new IllegalStateException("Already in TrialScope");
      }
      contextLocal.set(TrialContext.this);
      try {
        return callable.call();
      } finally {
        contextLocal.remove();
      }
    }
  }

  /**
   * Makes a new TrialContext that can be used to invoke
   */
  static TrialContext makeContext(UUID trialId, int trialNumber, Experiment experiment) {
    return new TrialContext(trialId, trialNumber, experiment);
  }

  private static final Scope SCOPE = new Scope() {
    @Override public <T> Provider<T> scope(final Key<T> key, final Provider<T> unscoped) {
      return new Provider<T>() {
        @Override public T get() {
          TrialContext context = contextLocal.get();
          if (context == null) {
            throw new OutOfScopeException("Not currently in TrialScope");
          }
          try {
            @SuppressWarnings("unchecked")
            T value = (T) context.contextMap.get(key, new Callable<T>() {
              @Override public T call() throws Exception {
                return unscoped.get();
              }
            });
            return value;
          } catch (ExecutionException e) {
            throw new ProvisionException("Unable to provide: " + key, e.getCause());
          }
        }
      };
    }
  };


  private static final Module INTERNAL = new AbstractModule() {
    @Override protected void configure() {
      bindScope(TrialScoped.class, SCOPE);
      // need to bind these with dummy providers here so Guice knows to use our scope to resolve 
      // them
      bindSeedKey(Key.get(UUID.class, TrialId.class));
      bindSeedKey(Key.get(Integer.class, TrialNumber.class));
      bindSeedKey(Key.get(Experiment.class));
    }

    private <T> void bindSeedKey(final Key<T> key) {
      bind(key).toProvider(new Provider<T>() {
        @Override public T get() {
          throw new OutOfScopeException(key 
              + " is not available because you are not in a TrialScope");
        }
      }).in(TrialScoped.class);
    }
  };
}
