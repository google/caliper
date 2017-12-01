/*
 * Copyright (C) 2011 Google Inc.
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

import com.google.caliper.runner.Instrument.InstrumentedMethod;
import com.google.common.base.Function;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableSetMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.SetMultimap;
import com.google.common.collect.Sets;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import javax.inject.Inject;

/**
 * A set of {@link Experiment experiments} constructed by taking all possible combinations of
 * instruments, benchmark methods, user parameters and targets.
 */
public final class ExperimentSelector {
  private final ImmutableSet<InstrumentedMethod> instrumentedMethods;
  private final ImmutableSet<Target> targets;
  private final ImmutableSetMultimap<String, String> userParameters;

  @Inject
  ExperimentSelector(
      ImmutableSet<InstrumentedMethod> instrumentedMethods,
      ImmutableSet<Target> targets,
      @BenchmarkParameters ImmutableSetMultimap<String, String> userParameters) {
    this.instrumentedMethods = instrumentedMethods;
    this.targets = targets;
    this.userParameters = userParameters;
  }

  // TODO(gak): put this someplace more sensible
  /** Returns the set of instruments to be used for benchmarking. */
  public ImmutableSet<Instrument> instruments() {
    return FluentIterable.from(instrumentedMethods)
        .transform(
            new Function<InstrumentedMethod, Instrument>() {
              @Override
              public Instrument apply(InstrumentedMethod input) {
                return input.instrument();
              }
            })
        .toSet();
  }

  /** Returns the targets that experiments will be run on. */
  public ImmutableSet<Target> targets() {
    return targets;
  }

  /** Returns the complete set of user benchmark parameters to use. */
  public ImmutableSetMultimap<String, String> userParameters() {
    return userParameters;
  }

  /** Returns the full set of experiments to be run. */
  public ImmutableSet<Experiment> selectExperiments() {
    List<Experiment> experiments = Lists.newArrayList();
    for (InstrumentedMethod instrumentedMethod : instrumentedMethods) {
      for (Target target : targets) {
        for (List<String> userParamsChoice : cartesian(userParameters)) {
          ImmutableMap<String, String> theseUserParams =
              zip(userParameters.keySet(), userParamsChoice);
          experiments.add(Experiment.create(instrumentedMethod, theseUserParams, target));
        }
      }
    }
    return ImmutableSet.copyOf(experiments);
  }

  protected static <T> Set<List<T>> cartesian(SetMultimap<String, T> multimap) {
    @SuppressWarnings({"unchecked", "rawtypes"}) // promised by spec
    ImmutableMap<String, Set<T>> paramsAsMap = (ImmutableMap) multimap.asMap();
    return Sets.cartesianProduct(paramsAsMap.values().asList());
  }

  protected static <K, V> ImmutableMap<K, V> zip(Set<K> keys, Collection<V> values) {
    ImmutableMap.Builder<K, V> builder = ImmutableMap.builder();

    Iterator<K> keyIterator = keys.iterator();
    Iterator<V> valueIterator = values.iterator();

    while (keyIterator.hasNext() && valueIterator.hasNext()) {
      builder.put(keyIterator.next(), valueIterator.next());
    }

    if (keyIterator.hasNext() || valueIterator.hasNext()) {
      throw new AssertionError(); // I really screwed up, then.
    }
    return builder.build();
  }
}
