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

package com.google.caliper.util;

import com.google.common.collect.ImmutableBiMap;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import com.google.common.io.ByteSource;
import com.google.common.io.Closer;
import com.google.common.io.Resources;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Member;
import java.lang.reflect.Modifier;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

public final class Util {
  private Util() {}

  // Users have no idea that nested classes are identified with '$', not '.', so if class lookup
  // fails try replacing the last . with $.
  public static Class<?> lenientClassForName(String className) throws ClassNotFoundException {
    try {
      return loadClass(className);
    } catch (ClassNotFoundException ignored) {
      // try replacing the last dot with a $, in case that helps
      // example: tutorial.Tutorial.Benchmark1 becomes tutorial.Tutorial$Benchmark1
      // amusingly, the $ character means three different things in this one line alone
      String newName = className.replaceFirst("\\.([^.]+)$", "\\$$1");
      return loadClass(newName);
    }
  }

  /**
   * Search for a class by name.
   *
   * @param className the name of the class.
   * @return the class.
   * @throws ClassNotFoundException if the class could not be found.
   */
  public static Class<?> loadClass(String className) throws ClassNotFoundException {
    // Use the thread context class loader. This is necessary because in some configurations, e.g.
    // when run from a single JAR containing caliper and all its dependencies the caliper JAR
    // ends up on the boot class path of the Worker and so needs to the use thread context class
    // loader to load classes provided by the user.
    return Class.forName(className, true, Thread.currentThread().getContextClassLoader());
  }

  public static ImmutableMap<String, String> loadProperties(ByteSource is) throws IOException {
    Properties props = new Properties();
    Closer closer = Closer.create();
    InputStream in = closer.register(is.openStream());
    try {
      props.load(in);
    } finally {
      closer.close();
    }
    return Maps.fromProperties(props);
  }

  public static ByteSource resourceSupplier(final Class<?> c, final String name) {
    return Resources.asByteSource(c.getResource(name));
  }

  private static <T> ImmutableMap<String, T> prefixedSubmap(
      Map<String, T> props, String prefix) {
    ImmutableMap.Builder<String, T> submapBuilder = ImmutableMap.builder();
    for (Map.Entry<String, T> entry : props.entrySet()) {
      String name = entry.getKey();
      if (name.startsWith(prefix)) {
        submapBuilder.put(name.substring(prefix.length()), entry.getValue());
      }
    }
    return submapBuilder.build();
  }

  /**
   * Returns a map containing only those entries whose key starts with {@code <groupName>.}.
   *
   * <p>The keys in the returned map have had their {@code <groupName>.} prefix removed.
   *
   * <p>e.g. If given a map that contained {@code group.key1 -> value1, key2 -> value2} and a
   * {@code groupName} of {@code group} it would produce a map containing {@code key1 -> value1}.
   */
  public static ImmutableMap<String, String> subgroupMap(
          Map<String, String> map, String groupName) {
    return prefixedSubmap(map, groupName + ".");
  }

  public static boolean isPublic(Member member) {
    return Modifier.isPublic(member.getModifiers());
  }

  public static boolean isStatic(Member member) {
    return Modifier.isStatic(member.getModifiers());
  }

  private static final long FORCE_GC_TIMEOUT_SECS = 2;

  public static void forceGc() {
    System.gc();
    System.runFinalization();
    final CountDownLatch latch = new CountDownLatch(1);
    new Object() {
      @Override protected void finalize() {
        latch.countDown();
      }
    };
    System.gc();
    System.runFinalization();
    try {
      latch.await(FORCE_GC_TIMEOUT_SECS, TimeUnit.SECONDS);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
    }
  }

  public static <T> ImmutableBiMap<T, String> assignNames(Set<T> items) {
    ImmutableList<T> itemList = ImmutableList.copyOf(items);
    ImmutableBiMap.Builder<T, String> itemNamesBuilder = ImmutableBiMap.builder();
    for (int i = 0; i < itemList.size(); i++) {
      itemNamesBuilder.put(itemList.get(i), generateUniqueName(i));
    }
    return itemNamesBuilder.build();
  }

  private static String generateUniqueName(int index) {
    if (index < 26) {
      return String.valueOf((char) ('A' + index));
    } else {
      return generateUniqueName(index / 26 - 1) + generateUniqueName(index % 26);
    }
  }
}
