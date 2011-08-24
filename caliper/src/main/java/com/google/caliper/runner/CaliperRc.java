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

import com.google.caliper.util.Util;
import com.google.common.base.Predicates;
import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Iterables;
import com.google.common.collect.Maps;

import java.util.List;
import java.util.Map;

public final class CaliperRc {
  static CaliperRc create(Map<String, String> overrides, Map<String, String> defaults) {
    Map<String, String> map = Maps.newHashMap(defaults);
    map.putAll(overrides); // overwrite and augment
    Iterables.removeIf(map.values(), Predicates.equalTo(""));
    return new CaliperRc(map);
  }

  private final ImmutableMap<String, String> props;

  public CaliperRc(Map<String, String> props) {
    this.props = ImmutableMap.copyOf(props);
  }

  public String vmBaseDirectory() {
    return props.get("vm.baseDirectory");
  }

  public ImmutableMap<String, String> globalDefaultVmArgs() {
    return submap("vm.args.jdk"); // TODO: android etc.
  }

  public String homeDirForVm(String name) {
    return props.get("vm." + name + ".home");
  }

  public ImmutableMap<String, String> vmArgsForVm(String vmName) {
    return submap("vm." + vmName + ".args");
  }

  public List<String> verboseArgsForVm(String vmName) {
    String verboseArgs = props.get("vm." + vmName + ".verboseMode");
    if (verboseArgs == null) {
      return ImmutableList.of();
    } else {
      return ImmutableList.copyOf(Splitter.on(' ').split(verboseArgs));
    }
  }

  public String instrumentClassName(String instrumentName) {
    return props.get("instrument." + instrumentName + ".class");
  }

  public ImmutableMap<String, String> instrumentOptions(String instrumentName) {
    return submap("instrument." + instrumentName + ".options");
  }

  public ImmutableMap<String, String> vmArgsForInstrument(String instrumentName) {
    return submap("instrument." + instrumentName + ".vmArgs.jdk"); // TODO: android etc.
  }

  private ImmutableMap<String, String> submap(String name) {
    return Util.prefixedSubmap(props, name + ".");
  }

}
