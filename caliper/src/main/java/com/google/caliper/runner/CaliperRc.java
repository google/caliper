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
import com.google.common.collect.ImmutableMap;

import java.io.File;
import java.util.Map;

/**
 * @author Kevin Bourrillion
 */
public class CaliperRc {
  private final ImmutableMap<String, String> props;
  private final ImmutableMap<String, String> vmAliases;
  private final ImmutableMap<String, String> instrumentAliases;

  private static final String VM_ALIAS_PREFIX = "vm.alias.";
  private static final String INSTRUMENT_ALIAS_PREFIX = "instrument.alias.";

  public CaliperRc(Map<String, String> props) {
    this.props = ImmutableMap.copyOf(props);

    vmAliases = Util.getPrefixedSubmap(props, VM_ALIAS_PREFIX);
    instrumentAliases = Util.getPrefixedSubmap(props, INSTRUMENT_ALIAS_PREFIX);
  }

  public File vmBaseDirectory() {
    String str = props.get("vm.baseDirectory");
    return (str != null) ? new File(str) : null;
  }

  public String vmCommonArguments() {
    return props.get("vm.commonArguments");
  }

  /**
   * Returns additional VM arguments to be used only in -l mode (detailed logging).
   */
  public String vmDetailArguments() {
    return props.get("vm.detailArguments");
  }

  public ImmutableMap<String, String> vmAliases() {
    return vmAliases;
  }

  public ImmutableMap<String, String> instrumentAliases() {
    return instrumentAliases;
  }

  public int defaultWarmupSeconds() {
    String str = props.get("instrument.microbenchmark.defaultWarmupSeconds");
    return (str == null) ? 10 : Integer.parseInt(str);
  }
}
