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

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.StandardSystemProperty.JAVA_CLASS_PATH;
import static com.google.common.base.StandardSystemProperty.PATH_SEPARATOR;
import static java.util.logging.Level.WARNING;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
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
 * Scans the source of a {@link ClassLoader} and finds all jar files. This is a modified version of
 * {@link com.google.common.reflect.ClassPath} that finds jars instead of resources.
 */
final class JarFinder {
  private static final Logger logger = Logger.getLogger(JarFinder.class.getName());

  /** Separator for the Class-Path manifest attribute value in jar files. */
  private static final Splitter CLASS_PATH_ATTRIBUTE_SEPARATOR =
      Splitter.on(' ').omitEmptyStrings();

  /**
   * Returns a list of jar files reachable from the given class loaders.
   *
   * <p><b>Warning:</b> Current limitations:
   *
   * <ul>
   *   <li>Looks only for files and JARs in URLs available from {@link URLClassLoader} instances or
   *       the {@linkplain ClassLoader#getSystemClassLoader() system class loader}.
   *   <li>Only understands {@code file:} URLs.
   * </ul>
   *
   * @throws IOException if the attempt to read class path resources (jar files or directories)
   *     failed.
   */
  public static ImmutableSet<File> findJarFiles(ClassLoader first, ClassLoader... rest)
      throws IOException {
    Scanner scanner = new Scanner();
    Map<File, ClassLoader> map = Maps.newLinkedHashMap();
    for (ClassLoader classLoader : Lists.asList(first, rest)) {
      map.putAll(getClassPathEntries(classLoader));
    }
    for (Map.Entry<File, ClassLoader> entry : map.entrySet()) {
      scanner.scan(entry.getKey(), entry.getValue());
    }
    return scanner.jarFiles();
  }

  @VisibleForTesting
  static ImmutableMap<File, ClassLoader> getClassPathEntries(ClassLoader classloader) {
    Map<File, ClassLoader> entries = Maps.newLinkedHashMap();
    // Search parent first, since it's the order ClassLoader#loadClass() uses.
    ClassLoader parent = classloader.getParent();
    if (parent != null) {
      entries.putAll(getClassPathEntries(parent));
    }
    for (URL url : getClassLoaderUrls(classloader)) {
      if (url.getProtocol().equals("file")) {
        File file = toFile(url);
        if (!entries.containsKey(file)) {
          entries.put(file, classloader);
        }
      }
    }
    return ImmutableMap.copyOf(entries);
  }

  private static ImmutableList<URL> getClassLoaderUrls(ClassLoader classloader) {
    if (classloader instanceof URLClassLoader) {
      return ImmutableList.copyOf(((URLClassLoader) classloader).getURLs());
    }
    if (classloader.equals(ClassLoader.getSystemClassLoader())) {
      return parseJavaClassPath();
    }
    return ImmutableList.of();
  }

  /**
   * Returns the URLs in the class path specified by the {@code java.class.path} {@linkplain
   * System#getProperty system property}.
   */
  // TODO(b/65488446): Use the public API once it's available.
  private static ImmutableList<URL> parseJavaClassPath() {
    ImmutableList.Builder<URL> urls = ImmutableList.builder();
    for (String entry : Splitter.on(PATH_SEPARATOR.value()).split(JAVA_CLASS_PATH.value())) {
      try {
        try {
          urls.add(new File(entry).toURI().toURL());
        } catch (SecurityException e) { // File.toURI checks to see if the file is a directory
          urls.add(new URL("file", null, new File(entry).getAbsolutePath()));
        }
      } catch (MalformedURLException e) {
        logger.log(WARNING, "malformed classpath entry: " + entry, e);
      }
    }
    return urls.build();
  }

  @VisibleForTesting
  static final class Scanner {
    private final ImmutableSet.Builder<File> jarFiles = new ImmutableSet.Builder<File>();
    private final Set<File> scannedFiles = Sets.newHashSet();

    ImmutableSet<File> jarFiles() {
      return jarFiles.build();
    }

    void scan(File file, ClassLoader classloader) throws IOException {
      if (scannedFiles.add(file)) {
        scanFrom(file, classloader);
      }
    }

    @VisibleForTesting
    void scanFrom(File file, ClassLoader classloader) throws IOException {
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

    private void scanDirectory(File directory, ClassLoader classloader, String packagePrefix) {
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
        for (File path : getClassPathFromManifest(file, jarFile.getManifest())) {
          scan(path, classloader);
        }
      } finally {
        try {
          jarFile.close();
        } catch (IOException ignored) {
        }
      }
    }

    /**
     * Returns the class path URIs specified by the {@code Class-Path} manifest attribute, according
     * to <a
     * href="http://docs.oracle.com/javase/8/docs/technotes/guides/jar/jar.html#Main_Attributes">JAR
     * File Specification</a>. If {@code manifest} is null, it means the jar file has no manifest,
     * and an empty set will be returned.
     */
    @VisibleForTesting
    static ImmutableSet<File> getClassPathFromManifest(File jarFile, @Nullable Manifest manifest) {
      if (manifest == null) {
        return ImmutableSet.of();
      }
      ImmutableSet.Builder<File> builder = ImmutableSet.builder();
      String classpathAttribute =
          manifest.getMainAttributes().getValue(Attributes.Name.CLASS_PATH.toString());
      if (classpathAttribute != null) {
        for (String path : CLASS_PATH_ATTRIBUTE_SEPARATOR.split(classpathAttribute)) {
          URL url;
          try {
            url = getClassPathEntry(jarFile, path);
          } catch (MalformedURLException e) {
            // Ignore bad entry
            logger.warning("Invalid Class-Path entry: " + path);
            continue;
          }
          if (url.getProtocol().equals("file")) {
            builder.add(toFile(url));
          }
        }
      }
      return builder.build();
    }

    /**
     * Returns the absolute uri of the Class-Path entry value as specified in <a
     * href="http://docs.oracle.com/javase/8/docs/technotes/guides/jar/jar.html#Main_Attributes">JAR
     * File Specification</a>. Even though the specification only talks about relative urls,
     * absolute urls are actually supported too (for example, in Maven surefire plugin).
     */
    @VisibleForTesting
    static URL getClassPathEntry(File jarFile, String path) throws MalformedURLException {
      return new URL(jarFile.toURI().toURL(), path);
    }
  }

  private static File toFile(URL url) {
    checkArgument(url.getProtocol().equals("file"));
    try {
      return new File(url.toURI()); // Accepts escaped characters like %20.
    } catch (URISyntaxException e) { // URL.toURI() doesn't escape chars.
      return new File(url.getPath()); // Accepts non-escaped chars like space.
    }
  }
}
