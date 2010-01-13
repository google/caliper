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
import java.util.Map;

public class PrintEnvironment {
  private PrintEnvironment() {}

  public static void main(String[] args) {
    @SuppressWarnings("unchecked")
    Map<String, String> sysProps = (Map<String, String>) (Map) System.getProperties();

    // Sometimes java.runtime.version is more descriptive than java.version
    String version = sysProps.get("java.version");
    String alternateVersion = sysProps.get("java.runtime.version");
    if (alternateVersion != null && alternateVersion.length() > version.length()) {
      version = alternateVersion;
    }
    System.out.println("jre.version=" + version);

    System.out.println("jre.vmname=" + sysProps.get("java.vm.name"));
    System.out.println("jre.vmversion=" + sysProps.get("java.vm.version"));
    System.out.println("jre.availableProcessors=" + Runtime.getRuntime().availableProcessors());

    String osName = sysProps.get("os.name");
    System.out.println("os.name=" + osName);
    System.out.println("os.version=" + sysProps.get("os.version"));
    System.out.println("os.arch=" + sysProps.get("os.arch"));

    try {
      System.out.println("host.name=" + InetAddress.getLocalHost().getHostName());
    } catch (UnknownHostException ignored) {
    }

    if (osName.equals("Linux")) { // the following probably doesn't work on ALL linux
      Multimap<String, String> cpuInfo = propertiesFromLinuxFile("/proc/cpuinfo");
      System.out.println("host.cpus=" + cpuInfo.get("processor").size());
      String s = "cpu cores";
      System.out.println("host.cpu.cores=" + describe(cpuInfo, s));
      System.out.println("host.cpu.speeds=" + describe(cpuInfo, "cpu MHz"));
      System.out.println("host.cpu.names=" + describe(cpuInfo, "model name"));
      System.out.println("host.cpu.cachesize=" + describe(cpuInfo, "cache size"));

      Multimap<String, String> memInfo = propertiesFromLinuxFile("/proc/meminfo");
      System.out.println("host.memory.physical=" + memInfo.get("MemTotal"));
      System.out.println("host.memory.swap=" + memInfo.get("SwapTotal"));
    }
  }

  private static String describe(Multimap<String, String> cpuInfo, String s) {
    Collection<String> strings = cpuInfo.get(s);
    return (strings.size() == 1)
        ? strings.iterator().next()
        : ImmutableMultiset.copyOf(strings).toString();
  }

  /**
   * Returns the key/value pairs from the specified properties-file like
   * reader. Unlike standard Java properties files, {@code reader} is allowed
   * to list the same property multiple times. Comments etc. are unsupported.
   */
  static Multimap<String, String> propertiesFileToMultimap(Reader reader)
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

  static Multimap<String, String> propertiesFromLinuxFile(String file) {
    try {
      Process process = Runtime.getRuntime().exec(new String[]{"/bin/cat", file});
      return propertiesFileToMultimap(
          new InputStreamReader(process.getInputStream(), "ISO-8859-1"));
    } catch (IOException e) {
      return ImmutableMultimap.of();
    }
  }
}