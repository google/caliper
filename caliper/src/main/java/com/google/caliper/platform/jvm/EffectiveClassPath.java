/*
 * Copyright (C) 2013 Google Inc.
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

package com.google.caliper.platform.jvm;

import com.google.common.base.Joiner;
import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableSet;

import java.io.File;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.LinkedHashSet;
import java.util.Set;

import javax.annotation.Nullable;

/**
 * Provides a class path containing all of the jars present on the local machine that are referenced
 * by a given {@link ClassLoader}.
 */
final class EffectiveClassPath {
  private EffectiveClassPath() {}

  private static final String PATH_SEPARATOR = System.getProperty("path.separator");
  private static final String JAVA_CLASS_PATH = System.getProperty("java.class.path");

  static String getClassPathForClassLoader(ClassLoader classLoader) {
    // Order of JAR files may have some significance. Try to preserve it.
    LinkedHashSet<File> files = new LinkedHashSet<File>();
    for (String entry : Splitter.on(PATH_SEPARATOR).split(JAVA_CLASS_PATH)) {
      files.add(new File(entry));
    }
    files.addAll(getClassPathFiles(classLoader));

    return Joiner.on(PATH_SEPARATOR).join(files);
  }

  private static Set<File> getClassPathFiles(ClassLoader classLoader) {
    ImmutableSet.Builder<File> files = ImmutableSet.builder();
    @Nullable ClassLoader parent = classLoader.getParent();
    if (parent != null) {
      files.addAll(getClassPathFiles(parent));
    }
    if (classLoader instanceof URLClassLoader) {
      URLClassLoader urlClassLoader = (URLClassLoader) classLoader;
      for (URL url : urlClassLoader.getURLs()) {
        try {
          files.add(new File(url.toURI()));
        } catch (URISyntaxException e) {
          // skip it then
        } catch (IllegalArgumentException e) {
          // skip it then
        }
      }
    }
    return files.build();
  }
}
