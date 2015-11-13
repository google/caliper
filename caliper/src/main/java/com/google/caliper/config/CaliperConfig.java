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

package com.google.caliper.config;

import static com.google.caliper.util.Util.subgroupMap;
import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;

import com.google.caliper.api.ResultProcessor;
import com.google.caliper.platform.Platform;
import com.google.caliper.platform.VirtualMachineException;
import com.google.caliper.util.Util;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.MoreObjects;
import com.google.common.base.Strings;
import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.google.common.collect.ImmutableBiMap;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;

import java.io.File;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.annotation.Nullable;

/**
 * Represents caliper configuration.  By default, {@code ~/.caliper/config.properties} and
 * {@code global-config.properties}.
 *
 * @author gak@google.com (Gregory Kick)
 */
public final class CaliperConfig {
  @VisibleForTesting final ImmutableMap<String, String> properties;
  private final ImmutableMap<Class<? extends ResultProcessor>, ResultProcessorConfig>
      resultProcessorConfigs;

  @VisibleForTesting
  public CaliperConfig(ImmutableMap<String, String> properties)
      throws InvalidConfigurationException {
    this.properties = checkNotNull(properties);
    this.resultProcessorConfigs = findResultProcessorConfigs(subgroupMap(properties, "results"));
  }

  private static final Pattern CLASS_PROPERTY_PATTERN = Pattern.compile("(\\w+)\\.class");

  private static <T> ImmutableBiMap<String, Class<? extends T>> mapGroupNamesToClasses(
      ImmutableMap<String, String> groupProperties, Class<T> type)
          throws InvalidConfigurationException {
    BiMap<String, Class<? extends T>> namesToClasses = HashBiMap.create();
    for (Entry<String, String> entry : groupProperties.entrySet()) {
      Matcher matcher = CLASS_PROPERTY_PATTERN.matcher(entry.getKey());
      if (matcher.matches() && !entry.getValue().isEmpty()) {
        try {
          Class<?> someClass = Util.loadClass(entry.getValue());
          checkState(type.isAssignableFrom(someClass));
          @SuppressWarnings("unchecked")
          Class<? extends T> verifiedClass = (Class<? extends T>) someClass;
          namesToClasses.put(matcher.group(1), verifiedClass);
        } catch (ClassNotFoundException e) {
          throw new InvalidConfigurationException("Cannot find result processor class: "
              + entry.getValue());
        }
      }
    }
    return ImmutableBiMap.copyOf(namesToClasses);
  }

  private static ImmutableMap<Class<? extends ResultProcessor>, ResultProcessorConfig>
      findResultProcessorConfigs(ImmutableMap<String, String> resultsProperties)
          throws InvalidConfigurationException {
    ImmutableBiMap<String, Class<? extends ResultProcessor>> processorToClass =
        mapGroupNamesToClasses(resultsProperties, ResultProcessor.class);
    ImmutableMap.Builder<Class<? extends ResultProcessor>, ResultProcessorConfig> builder =
        ImmutableMap.builder();
    for (Entry<String, Class<? extends ResultProcessor>> entry : processorToClass.entrySet()) {
      builder.put(entry.getValue(), getResultProcessorConfig(resultsProperties, entry.getKey()));
    }
    return builder.build();
  }

  public ImmutableMap<String, String> properties() {
    return properties;
  }

  /**
   * Returns the configuration of the current host VM (including the flags used to create it). Any
   * args specified using {@code vm.args} will also be applied
   */
  public VmConfig getDefaultVmConfig(Platform platform) {
    return new VmConfig.Builder(platform, platform.defaultVmHomeDir())
        .addAllOptions(platform.inputArguments())
        // still incorporate vm.args
        .addAllOptions(getArgs(subgroupMap(properties, "vm")))
        .build();
  }

  public VmConfig getVmConfig(Platform platform, String name)
      throws InvalidConfigurationException {
    checkNotNull(name);
    ImmutableMap<String, String> vmGroupMap = subgroupMap(properties, "vm");
    ImmutableMap<String, String> vmMap = subgroupMap(vmGroupMap, name);
    File homeDir;
    try {
      homeDir = platform.customVmHomeDir(vmGroupMap, name);
    } catch (VirtualMachineException e) {
      throw new InvalidConfigurationException(e);
    }
    return new VmConfig.Builder(platform, homeDir)
        .addAllOptions(getArgs(vmGroupMap))
        .addAllOptions(getArgs(vmMap))
        .build();
  }

  private static final Pattern INSTRUMENT_CLASS_PATTERN = Pattern.compile("([^\\.]+)\\.class");

  public ImmutableSet<String> getConfiguredInstruments() {
    ImmutableSet.Builder<String> resultBuilder = ImmutableSet.builder();
    for (String key : subgroupMap(properties, "instrument").keySet()) {
      Matcher matcher = INSTRUMENT_CLASS_PATTERN.matcher(key);
      if (matcher.matches()) {
        resultBuilder.add(matcher.group(1));
      }
    }
    return resultBuilder.build();
  }

  public InstrumentConfig getInstrumentConfig(String name) {
    checkNotNull(name);
    ImmutableMap<String, String> instrumentGroupMap = subgroupMap(properties, "instrument");
    ImmutableMap<String, String> insrumentMap = subgroupMap(instrumentGroupMap, name);
    @Nullable String className = insrumentMap.get("class");
    checkArgument(className != null, "no instrument configured named %s", name);
    return new InstrumentConfig.Builder()
        .className(className)
        .addAllOptions(subgroupMap(insrumentMap, "options"))
        .build();
  }

  public ImmutableSet<Class<? extends ResultProcessor>> getConfiguredResultProcessors() {
    return resultProcessorConfigs.keySet();
  }

  public ResultProcessorConfig getResultProcessorConfig(
      Class<? extends ResultProcessor> resultProcessorClass) {
    return resultProcessorConfigs.get(resultProcessorClass);
  }

  private static ResultProcessorConfig getResultProcessorConfig(
      ImmutableMap<String, String> resultsProperties, String name) {
    ImmutableMap<String, String> resultsMap = subgroupMap(resultsProperties, name);
    return new ResultProcessorConfig.Builder()
        .className(resultsMap.get("class"))
        .addAllOptions(subgroupMap(resultsMap, "options"))
        .build();
  }

  @Override public String toString() {
    return MoreObjects.toStringHelper(this)
        .add("properties", properties)
        .toString();
  }

  private static List<String> getArgs(Map<String, String> properties) {
    String argsString = Strings.nullToEmpty(properties.get("args"));
    ImmutableList.Builder<String> args = ImmutableList.builder();
    StringBuilder arg = new StringBuilder();
    for (int i = 0; i < argsString.length(); i++) {
      char c = argsString.charAt(i);
      switch (c) {
        case '\\':
          arg.append(argsString.charAt(++i));
          break;
        case ' ':
          if (arg.length() > 0) {
            args.add(arg.toString());
          }
          arg = new StringBuilder();
          break;
        default:
          arg.append(c);
          break;
      }
    }
    if (arg.length() > 0) {
      args.add(arg.toString());
    }
    return args.build();
  }

}
