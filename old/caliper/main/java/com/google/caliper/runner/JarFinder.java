/*
 * Copyright (C) 2013 The Guava Authors
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

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.google.common.reflect.ClassPath;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Map;
import java.util.Set;
import java.util.jar.Attributes;
import java.util.jar.JarFile;
import java.util.jar.Manifest;
import java.util.logging.Logger;

import javax.annotation.Nullable;

/**
 * Scans the source of a {@link ClassLoader} and finds all jar files.  This is a modified version
 * of {@link ClassPath} that finds jars instead of resources.
 */
final class JarFinder {
  private static final Logger logger = Logger.getLogger(JarFinder.class.getName());

  /** Separator for the Class-Path manifest attribute value in jar files. */
  private static final Splitter CLASS_PATH_ATTRIBUTE_SEPARATOR =
      Splitter.on(' ').omitEmptyStrings();

  /**
   * Returns a list of jar files reachable from the given class loaders.
   *
   * <p>Currently only {@link URLClassLoader} and only {@code file://} urls are supported.
   *
   * @throws IOException if the attempt to read class path resources (jar files or directories)
   *         failed.
   */
  public static ImmutableSet<File> findJarFiles(ClassLoader first, ClassLoader... rest)
      throws IOException {
    Scanner scanner = new Scanner();
    Map<URI, ClassLoader> map = Maps.newLinkedHashMap();
    for (ClassLoader classLoader : Lists.asList(first, rest)) {
      map.putAll(getClassPathEntries(classLoader));
    }
    for (Map.Entry<URI, ClassLoader> entry : map.entrySet()) {
      scanner.scan(entry.getKey(), entry.getValue());
    }
    return scanner.jarFiles();
  }

  @VisibleForTesting static ImmutableMap<URI, ClassLoader> getClassPathEntries(
      ClassLoader classloader) {
    Map<URI, ClassLoader> entries = Maps.newLinkedHashMap();
    // Search parent first, since it's the order ClassLoader#loadClass() uses.
    ClassLoader parent = classloader.getParent();
    if (parent != null) {
      entries.putAll(getClassPathEntries(parent));
    }
    if (classloader instanceof URLClassLoader) {
      URLClassLoader urlClassLoader = (URLClassLoader) classloader;
      for (URL entry : urlClassLoader.getURLs()) {
        URI uri;
        try {
          uri = entry.toURI();
        } catch (URISyntaxException e) {
          throw new IllegalArgumentException(e);
        }
        if (!entries.containsKey(uri)) {
          entries.put(uri, classloader);
        }
      }
    }
    return ImmutableMap.copyOf(entries);
  }

  @VisibleForTesting static final class Scanner {
    private final ImmutableSet.Builder<File> jarFiles = new ImmutableSet.Builder<File>();
    private final Set<URI> scannedUris = Sets.newHashSet();

    ImmutableSet<File> jarFiles() {
      return jarFiles.build();
    }

    void scan(URI uri, ClassLoader classloader) throws IOException {
      if (uri.getScheme().equals("file") && scannedUris.add(uri)) {
        scanFrom(new File(uri), classloader);
      }
    }

    @VisibleForTesting void scanFrom(File file, ClassLoader classloader)
        throws IOException {
      if (!file.exists()) {
        return;
      }
      if (file.isDirectory()) {
        scanDirectory(file, classloader);
      } else {
        scanJar(file, classloader);
      }
    }

    private void scanDirectory(File directory, ClassLoader classloader) {
      scanDirectory(directory, classloader, "");
    }

    private void scanDirectory(
        File directory, ClassLoader classloader, String packagePrefix) {
      for (File file : directory.listFiles()) {
        String name = file.getName();
        if (file.isDirectory()) {
          scanDirectory(file, classloader, packagePrefix + name + "/");
        }
        // do we need to look for jars here?
      }
    }

    private void scanJar(File file, ClassLoader classloader) throws IOException {
      JarFile jarFile;
      try {
        jarFile = new JarFile(file);
      } catch (IOException e) {
        // Not a jar file
        return;
      }
      jarFiles.add(file);
      try {
        for (URI uri : getClassPathFromManifest(file, jarFile.getManifest())) {
          scan(uri, classloader);
        }
      } finally {
        try {
          jarFile.close();
        } catch (IOException ignored) {}
      }
    }

    /**
     * Returns the class path URIs specified by the {@code Class-Path} manifest attribute, according
     * to <a
     * href="http://docs.oracle.com/javase/6/docs/technotes/guides/jar/jar.html#Main%20Attributes">
     * JAR File Specification</a>. If {@code manifest} is null, it means the jar file has no
     * manifest, and an empty set will be returned.
     */
    @VisibleForTesting static ImmutableSet<URI> getClassPathFromManifest(
        File jarFile, @Nullable Manifest manifest) {
      if (manifest == null) {
        return ImmutableSet.of();
      }
      ImmutableSet.Builder<URI> builder = ImmutableSet.builder();
      String classpathAttribute = manifest.getMainAttributes()
          .getValue(Attributes.Name.CLASS_PATH.toString());
      if (classpathAttribute != null) {
        for (String path : CLASS_PATH_ATTRIBUTE_SEPARATOR.split(classpathAttribute)) {
          URI uri;
          try {
            uri = getClassPathEntry(jarFile, path);
          } catch (URISyntaxException e) {
            // Ignore bad entry
            logger.warning("Invalid Class-Path entry: " + path);
            continue;
          }
          builder.add(uri);
        }
      }
      return builder.build();
    }

    /**
     * Returns the absolute uri of the Class-Path entry value as specified in
     * <a
     * href="http://docs.oracle.com/javase/6/docs/technotes/guides/jar/jar.html#Main%20Attributes">
     * JAR File Specification</a>. Even though the specification only talks about relative urls,
     * absolute urls are actually supported too (for example, in Maven surefire plugin).
     */
    @VisibleForTesting static URI getClassPathEntry(File jarFile, String path)
        throws URISyntaxException {
      URI uri = new URI(path);
      return uri.isAbsolute()
          ? uri
          : new File(jarFile.getParentFile(), path.replace('/', File.separatorChar)).toURI();
    }
  }
}
