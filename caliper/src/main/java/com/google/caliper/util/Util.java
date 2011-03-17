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

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import com.google.common.io.Closeables;
import com.google.common.io.InputSupplier;
import com.google.common.primitives.Primitives;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Map;
import java.util.Properties;

public final class Util {
  private Util() {}

  // Users have no idea that nested classes are identified with '$', not '.', so if class lookup
  // fails try replacing the last . with $.
  public static Class<?> lenientClassForName(String className) throws ClassNotFoundException {
    try {
      return Class.forName(className);
    } catch (ClassNotFoundException ignored) {
      // try replacing the last dot with a $, in case that helps
      // example: tutorial.Tutorial.Benchmark1 becomes tutorial.Tutorial$Benchmark1
      // amusingly, the $ character means three different things in this one line alone
      String newName = className.replaceFirst("\\.([^.]+)$", "\\$$1");
      return Class.forName(newName);
    }
  }

  public static ImmutableMap<String, String> loadProperties(
      InputSupplier<? extends InputStream> is) throws IOException {
    Properties props = new Properties();
    InputStream in = is.getInput();
    try {
      props.load(in);
    } finally {
      Closeables.closeQuietly(in);
    }
    return Maps.fromProperties(props);
  }

  // TODO: this is similar to Resources.getResource
  public static InputSupplier<InputStream> resourceSupplier(final Class<?> c, final String name) {
    return new InputSupplier<InputStream>() {
      @Override public InputStream getInput() {
        return c.getResourceAsStream(name);
      }
    };
  }

  // TODO: replace with common.text.Parser when in Guava

  public static boolean extendsIgnoringWrapping(Class<?> possibleSub, Class<?> possibleSuper) {
    return Primitives.wrap(possibleSuper).isAssignableFrom(Primitives.wrap(possibleSub));
  }

  public static ImmutableMap<String, String> getPrefixedSubmap(
      Map<String, String> props, String prefix) {
    ImmutableMap.Builder<String, String> submapBuilder = ImmutableMap.builder();
    for (Map.Entry<String, String> entry : props.entrySet()) {
      String name = entry.getKey();
      if (name.startsWith(prefix)) {
        submapBuilder.put(name.substring(prefix.length()), entry.getValue());
      }
    }
    return submapBuilder.build();
  }

  public static boolean isPublic(Member member) {
    return Modifier.isPublic(member.getModifiers());
  }

  public static boolean isStatic(Member member) {
    return Modifier.isStatic(member.getModifiers());
  }

  @SuppressWarnings("unchecked") // checked manually
  public static <T> ImmutableList<T> checkedCast(ImmutableList<?> source, Class<T> toType) {
    for (Object o : source) {
      toType.cast(o);
    }
    return (ImmutableList<T>) source;
  }
}