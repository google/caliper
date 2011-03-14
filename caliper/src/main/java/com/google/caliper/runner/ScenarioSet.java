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

import static com.google.common.base.Preconditions.checkArgument;

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

/**
 * A set of {@link Scenario scenarios} constructed by taking all possible combinations of benchmark
 * methods, user parameters, VM specs and VM arguments.
 */
public final class ScenarioSet implements Iterable<Scenario> {
  private final ImmutableSet<BenchmarkMethod> allBenchmarkMethods;
  private final ImmutableSet<VirtualMachine> allVms;
  private final ImmutableSetMultimap<String, String> allUserParameters;
  private final ImmutableSetMultimap<String, String> allVmArguments;

  private final ImmutableSet<Scenario> allScenarios;

  public ScenarioSet(
      Collection<BenchmarkMethod> allBenchmarkMethods,
      Collection<VirtualMachine> allVms,
      SetMultimap<String, String> allUserParameters,
      SetMultimap<String, String> allVmArguments) {
    this.allBenchmarkMethods = ImmutableSet.copyOf(allBenchmarkMethods);
    this.allVms = ImmutableSet.copyOf(allVms);
    this.allUserParameters = ImmutableSetMultimap.copyOf(allUserParameters);
    this.allVmArguments = ImmutableSetMultimap.copyOf(allVmArguments);

    checkArgument(!allBenchmarkMethods.isEmpty());
    checkArgument(!allVms.isEmpty());

    this.allScenarios = makeScenarios();
  }

  public int size() {
    int size = allBenchmarkMethods.size();
    size *= allVms.size();
    for (Collection<String> entry : allUserParameters.asMap().values()) {
      size *= entry.size();
    }
    for (Collection<String> entry : allVmArguments.asMap().values()) {
      size *= entry.size();
    }
    return size;
  }

  public Iterator<Scenario> iterator() {
    return allScenarios.iterator();
  }

  private ImmutableSet<Scenario> makeScenarios() {
    List<Scenario> tmp = Lists.newArrayListWithCapacity(size());
    for (BenchmarkMethod benchmarkMethod : allBenchmarkMethods) {
      for (VirtualMachine vm : allVms) {
        for (List<String> userParamsChoice : cartesian(allUserParameters)) {
          ImmutableMap<String, String> theseUserParams =
              zip(allVmArguments.keySet(), userParamsChoice);
          for (List<String> vmArgsChoice : cartesian(allVmArguments)) {
            ImmutableMap<String, String> theseVmArgs =
                zip(allVmArguments.keySet(), vmArgsChoice);
            tmp.add(new Scenario(benchmarkMethod, theseUserParams, theseVmArgs, vm));
          }
        }
      }
    }
    return ImmutableSet.copyOf(tmp);
  }

  private static Set<List<String>> cartesian(SetMultimap<String, String> multimap) {
    @SuppressWarnings("unchecked") // promised by spec
    ImmutableMap<String, Set<String>> paramsAsMap = (ImmutableMap) multimap.asMap();
    return Sets.cartesianProduct(paramsAsMap.values().asList());
  }

  private static <K, V> ImmutableMap<K, V> zip(Set<K> keys, Collection<V> values) {
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
