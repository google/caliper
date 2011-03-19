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
public final class FullCartesianScenarioSelection implements ScenarioSelection {
  private final ImmutableSet<BenchmarkMethod> benchmarkMethods;
  private final ImmutableSet<VirtualMachine> vms;
  private final ImmutableSetMultimap<String, String> userParameters;
  private final ImmutableSetMultimap<String, String> vmArguments;

  public FullCartesianScenarioSelection(
      Collection<BenchmarkMethod> benchmarkMethods,
      Collection<VirtualMachine> vms,
      SetMultimap<String, String> userParameters,
      SetMultimap<String, String> vmArguments) {
    this.benchmarkMethods = ImmutableSet.copyOf(benchmarkMethods);
    this.vms = ImmutableSet.copyOf(vms);
    this.userParameters = ImmutableSetMultimap.copyOf(userParameters);
    this.vmArguments = ImmutableSetMultimap.copyOf(vmArguments);

    checkArgument(!benchmarkMethods.isEmpty());
    checkArgument(!vms.isEmpty());
  }

  @Override public ImmutableSet<BenchmarkMethod> benchmarkMethods() {
    return benchmarkMethods;
  }

  @Override public ImmutableSet<VirtualMachine> vms() {
    return vms;
  }

  @Override public ImmutableSetMultimap<String, String> userParameters() {
    return userParameters;
  }

  @Override public ImmutableSetMultimap<String, String> vmArguments() {
    return vmArguments;
  }

  @Override public ImmutableSet<Scenario> buildScenarios() {
    List<Scenario> tmp = Lists.newArrayList();
    for (BenchmarkMethod benchmarkMethod : benchmarkMethods) {
      for (VirtualMachine vm : vms) {
        for (List<String> userParamsChoice : cartesian(userParameters)) {
          ImmutableMap<String, String> theseUserParams =
              zip(userParameters.keySet(), userParamsChoice);
          for (List<String> vmArgsChoice : cartesian(vmArguments)) {
            ImmutableMap<String, String> theseVmArgs =
                zip(vmArguments.keySet(), vmArgsChoice);
            tmp.add(new Scenario(benchmarkMethod, theseUserParams, theseVmArgs, vm));
          }
        }
      }
    }
    return ImmutableSet.copyOf(tmp);
  }

  protected static <T> Set<List<T>> cartesian(SetMultimap<String, T> multimap) {
    @SuppressWarnings("unchecked") // promised by spec
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
