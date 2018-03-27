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

package com.google.caliper.runner.config;

import static com.google.caliper.util.Util.subgroupMap;
import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;

import com.google.caliper.api.ResultProcessor;
import com.google.caliper.util.Util;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.MoreObjects;
import com.google.common.base.Splitter;
import com.google.common.base.Strings;
import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.google.common.collect.ImmutableBiMap;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.annotation.Nullable;

/**
 * Represents caliper configuration. By default, {@code ~/.caliper/config.properties} and {@code
 * global-config.properties}.
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
          throw new InvalidConfigurationException(
              "Cannot find result processor class: " + entry.getValue());
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

  public DeviceConfig getDeviceConfig(String deviceName) {
    ImmutableMap<String, String> devices = subgroupMap(properties, "device");
    ImmutableMap<String, String> device = subgroupMap(devices, deviceName);

    String deviceType = device.get("type");
    if (deviceType == null) {
      throw new InvalidConfigurationException(
          "Missing configuration field: device." + deviceName + ".type");
    }

    return DeviceConfig.builder()
        .name(deviceName)
        .type(DeviceType.of(deviceType))
        .options(subgroupMap(device, "options"))
        .build();
  }

  /** Returns the vm.args configuration values. */
  public List<String> getVmArgs() {
    return getArgs(subgroupMap(properties, "vm"));
  }

  public VmConfig getVmConfig(String name) {
    checkNotNull(name);
    ImmutableMap<String, String> vmGroupMap = subgroupMap(properties, "vm");
    ImmutableMap<String, String> vmMap = subgroupMap(vmGroupMap, name);
    VmConfig.Builder builder = VmConfig.builder().name(name);
    String type = vmMap.get("type");
    if (type != null) {
      builder.type(VmType.of(type));
    }
    String home = vmMap.get("home");
    if (home != null) {
      builder.home(home);
    }
    String executable = vmMap.get("executable");
    if (executable != null) {
      builder.executable(executable);
    }
    return builder.addAllArgs(getVmArgs()).addAllArgs(getArgs(vmMap)).build();
  }

  private static final Pattern INSTRUMENT_CLASS_PATTERN = Pattern.compile("([^\\.]+)\\.class");

  /**
   * Returns the default set of instruments to use if the user doesn't specify which instruments to
   * use on the command line.
   */
  public ImmutableSet<String> getDefaultInstruments() {
    // TODO(cgdecker): could/should this "defaults" be generalized?
    // e.g. if there's a command line option "--foo", "defaults.foo" is the default value of "foo"
    // if the user doesn't pass that option. This is already the case here since "--instrument" is
    // the long-form commmand line option, but I'm not trying to generalize now since there's no
    // apparent need to.
    ImmutableMap<String, String> defaults = subgroupMap(properties, "defaults");
    if (!defaults.isEmpty()) {
      String instruments = defaults.get("instrument");
      if (instruments != null) {
        return ImmutableSet.copyOf(Splitter.on(',').split(instruments));
      }
    }

    throw new InvalidConfigurationException(
        "Could not find default set of instruments to use (defaults.instrument in config file)");
  }

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
    ImmutableMap<String, String> instrumentMap = subgroupMap(instrumentGroupMap, name);
    @Nullable String className = instrumentMap.get("class");
    checkArgument(className != null, "no instrument configured named %s", name);
    return new InstrumentConfig.Builder()
        .className(className)
        .addAllOptions(subgroupMap(instrumentMap, "options"))
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

  @Override
  public String toString() {
    return MoreObjects.toStringHelper(this).add("properties", properties).toString();
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
