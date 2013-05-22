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

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableSetMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.SetMultimap;
import com.google.common.collect.Sets;
import com.google.inject.Inject;

import java.lang.reflect.Method;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;

/**
 * A set of {@link Experiment experiments} constructed by taking all possible combinations of
 * instruments, benchmark methods, user parameters, VM specs and VM arguments.
 */
public final class FullCartesianExperimentSelector implements ExperimentSelector {
  private ImmutableSetMultimap<Instrument, Method> benchmarkMethodsByInstrument;
  private final ImmutableSet<VirtualMachine> vms;
  private final ImmutableSetMultimap<String, String> userParameters;

  @Inject FullCartesianExperimentSelector(
      ImmutableSetMultimap<Instrument, Method> benchmarkMethodsByInstrument,
      ImmutableSet<VirtualMachine> vms,
      @BenchmarkParameters ImmutableSetMultimap<String, String> userParameters) {
    this.benchmarkMethodsByInstrument = benchmarkMethodsByInstrument;
    this.vms = vms;
    this.userParameters = userParameters;
  }

  @Override public ImmutableSet<Instrument> instruments() {
    return benchmarkMethodsByInstrument.keySet();
  }

  @Override public ImmutableSet<VirtualMachine> vms() {
    return vms;
  }

  @Override public ImmutableSetMultimap<String, String> userParameters() {
    return userParameters;
  }

  @Override public ImmutableSet<Experiment> selectExperiments() {
    List<Experiment> experiments = Lists.newArrayList();
    for (Entry<Instrument, Method> entry : benchmarkMethodsByInstrument.entries()) {
      for (VirtualMachine vm : vms) {
        for (List<String> userParamsChoice : cartesian(userParameters)) {
          ImmutableMap<String, String> theseUserParams =
              zip(userParameters.keySet(), userParamsChoice);
          experiments.add(
              new Experiment(entry.getKey(), entry.getValue(), theseUserParams, vm));
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

  @Override public String selectionType() {
    return "Full cartesian product";
  }
}
