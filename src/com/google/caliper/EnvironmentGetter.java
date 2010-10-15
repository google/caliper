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

package com.google.caliper;

import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.ImmutableMultiset;
import com.google.common.collect.Multimap;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public final class EnvironmentGetter {

  public Environment getEnvironmentSnapshot() {
    Map<String, String> propertyMap = new HashMap<String, String>();

    @SuppressWarnings("unchecked")
    Map<String, String> sysProps = (Map<String, String>) (Map) System.getProperties();

    // Sometimes java.runtime.version is more descriptive than java.version
    String version = sysProps.get("java.version");
    String alternateVersion = sysProps.get("java.runtime.version");
    if (alternateVersion != null && alternateVersion.length() > version.length()) {
      version = alternateVersion;
    }
    propertyMap.put("jre.version", version);

    propertyMap.put("jre.vmname", sysProps.get("java.vm.name"));
    propertyMap.put("jre.vmversion", sysProps.get("java.vm.version"));
    propertyMap.put("jre.availableProcessors",
        Integer.toString(Runtime.getRuntime().availableProcessors()));

    String osName = sysProps.get("os.name");
    propertyMap.put("os.name", osName);
    propertyMap.put("os.version", sysProps.get("os.version"));
    propertyMap.put("os.arch", sysProps.get("os.arch"));

    try {
      propertyMap.put("host.name", InetAddress.getLocalHost().getHostName());
    } catch (UnknownHostException ignored) {
    }

    if (osName.equals("Linux")) { // the following probably doesn't work on ALL linux
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

    return new Environment(propertyMap);
  }

  private static String describe(Multimap<String, String> cpuInfo, String s) {
    Collection<String> strings = cpuInfo.get(s);
    // TODO redo the ImmutableMultiset.toString() call so we don't get square brackets
    return (strings.size() == 1)
        ? strings.iterator().next()
        : ImmutableMultiset.copyOf(strings).toString();
  }

  /**
   * Returns the key/value pairs from the specified properties-file like
   * reader. Unlike standard Java properties files, {@code reader} is allowed
   * to list the same property multiple times. Comments etc. are unsupported.
   */
  private static Multimap<String, String> propertiesFileToMultimap(Reader reader)
      throws IOException {
    ImmutableMultimap.Builder<String, String> result = ImmutableMultimap.builder();
    BufferedReader in = new BufferedReader(reader);

    String line;
    while((line = in.readLine()) != null) {
      String[] parts = line.split("\\s*\\:\\s*", 2);
      if (parts.length == 2) {
        result.put(parts[0], parts[1]);
      }
    }

    return result.build();
  }

  private static Multimap<String, String> propertiesFromLinuxFile(String file) {
    try {
      Process process = Runtime.getRuntime().exec(new String[]{"/bin/cat", file});
      return propertiesFileToMultimap(
          new InputStreamReader(process.getInputStream(), "ISO-8859-1"));
    } catch (IOException e) {
      return ImmutableMultimap.of();
    }
  }
}
