/*
 * Copyright (C) 2010 Google Inc.
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

import com.google.caliper.model.Host;
import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.ImmutableMultiset;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;
import com.google.common.io.Files;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * An instance of this class is responsible for returning a Map that describes the environment:
 * JVM version, os details, etc.
 */
final class EnvironmentGetter {
  Host getHost() {
    return new Host.Builder()
        .addAllProperies(getProperties())
        .build();
  }
  
  private Map<String, String> getProperties() {
    TreeMap<String, String> propertyMap = Maps.newTreeMap();

    Map<String, String> sysProps = Maps.fromProperties(System.getProperties());

    // Sometimes java.runtime.version is more descriptive than java.version
    String version = sysProps.get("java.version");
    String alternateVersion = sysProps.get("java.runtime.version");
    if (alternateVersion != null && alternateVersion.length() > version.length()) {
      version = alternateVersion;
    }
    propertyMap.put("host.availableProcessors",
        Integer.toString(Runtime.getRuntime().availableProcessors()));

    String osName = sysProps.get("os.name");
    propertyMap.put("os.name", osName);
    propertyMap.put("os.version", sysProps.get("os.version"));
    propertyMap.put("os.arch", sysProps.get("os.arch"));

    if (osName.equals("Linux")) {
      getLinuxEnvironment(propertyMap);
    }

    return propertyMap;
  }

  private void getLinuxEnvironment(Map<String, String> propertyMap) {
    // the following probably doesn't work on ALL linux
    Multimap<String, String> cpuInfo = propertiesFromLinuxFile("/proc/cpuinfo");
    propertyMap.put("host.cpus", Integer.toString(cpuInfo.get("processor").size()));
    String s = "cpu cores";
    propertyMap.put("host.cpu.cores", describe(cpuInfo, s));
    propertyMap.put("host.cpu.names", describe(cpuInfo, "model name"));
    propertyMap.put("host.cpu.cachesize", describe(cpuInfo, "cache size"));

    Multimap<String, String> memInfo = propertiesFromLinuxFile("/proc/meminfo");
    // TODO redo memInfo.toString() so we don't get square brackets
    propertyMap.put("host.memory.physical", memInfo.get("MemTotal").toString());
    propertyMap.put("host.memory.swap", memInfo.get("SwapTotal").toString());
  }

  private static String describe(Multimap<String, String> cpuInfo, String s) {
    Collection<String> strings = cpuInfo.get(s);
    // TODO redo the ImmutableMultiset.toString() call so we don't get square brackets
    return (strings.size() == 1)
        ? strings.iterator().next()
        : ImmutableMultiset.copyOf(strings).toString();
  }

  /**
   * Returns the key/value pairs from the specified properties-file like file.
   * Unlike standard Java properties files, {@code reader} is allowed to list
   * the same property multiple times. Comments etc. are unsupported.
   *
   * <p>If there's any problem reading the file's contents, we'll return an
   * empty Multimap.
   */
  private static Multimap<String, String> propertiesFromLinuxFile(String file) {
    try {
      List<String> lines = Files.readLines(new File(file), Charset.defaultCharset());
      ImmutableMultimap.Builder<String, String> result = ImmutableMultimap.builder();
      for (String line : lines) {
        // TODO(schmoe): replace with Splitter (in Guava release 10)
        String[] parts = line.split("\\s*\\:\\s*", 2);
        if (parts.length == 2) {
          result.put(parts[0], parts[1]);
        }
      }
      return result.build();
    } catch (IOException e) {
      // If there's any problem reading the file, just return an empty multimap.
      return ImmutableMultimap.of();
    }
  }
}
