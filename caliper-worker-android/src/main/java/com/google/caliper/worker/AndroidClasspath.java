/*
 * Copyright (C) 2017 Google Inc.
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

package com.google.caliper.worker;

import android.content.pm.ApplicationInfo;
import com.google.common.base.Joiner;
import com.google.common.io.ByteStreams;
import com.google.common.io.Closer;
import java.io.Closeable;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * Utility for getting the classpath of the running process on Android in a form usable for
 * launching new Android VMs using the same classpath. If the version of Android does not have
 * native multidex support and the apk for the current application has multiple dexes, they may be
 * extracted to a new location for use.
 */
final class AndroidClasspath {

  /**
   * Gets the classpath that Caliper should use when launching worker processes.
   *
   * <p>By default (apk with a single dex or when multidex is natively supported), the classpath is
   * just the location of the apk containing this app. If the apk has multiple dexes and multidex
   * isn't natively supported, all dexes are extracted from the apk and the classpath contains the
   * path to each.
   */
  static String getClasspath(ApplicationInfo applicationInfo) throws IOException {
    String apk = applicationInfo.sourceDir;

    File codeCache = new File(applicationInfo.dataDir, "code_cache");

    // If the secondary-dexes dir exists, that means that
    // A) the apk is multidex, and
    // B) there's no native multidex support on this Android version
    File secondaryDexDir = new File(codeCache, "secondary-dexes");
    if (!secondaryDexDir.exists()) {
      return apk;
    }

    // Note: we don't use any dexes extracted to the secondary-dexes dir because they may have been
    // modified (e.g. dexopt'ed) in place, which will cause DexOpt to fail when trying to start a
    // worker process.
    List<File> dexFiles = extractDexes(apk, new File(codeCache, "worker-dexes"));

    return Joiner.on(System.getProperty("path.separator")).join(dexFiles);
  }

  /**
   * Extracts {@code classes.dex} and all secondary {@code classes<N>.dex} files from the given apk
   * file to the given target directory.
   */
  private static List<File> extractDexes(String apk, File targetDir) throws IOException {
    targetDir.mkdirs();
    List<File> results = new ArrayList<>();
    Closer closer = Closer.create();
    try {
      ZipFile zip = closer.register(new CloseableZipFile(apk)).zipFile;

      ZipEntry classesEntry = zip.getEntry("classes.dex");
      if (classesEntry == null) {
        throw new AssertionError("classes.dex not found in apk");
      }

      int i = 1;
      do {
        InputStream in = closer.register(zip.getInputStream(classesEntry));

        File outFile = new File(targetDir, classesEntry.getName());
        OutputStream out = closer.register(new FileOutputStream(outFile));

        ByteStreams.copy(in, out);

        results.add(outFile);

        classesEntry = zip.getEntry("classes" + (++i) + ".dex");
      } while (classesEntry != null);
      return results;
    } catch (Throwable t) {
      throw closer.rethrow(t);
    } finally {
      closer.close();
    }
  }

  /**
   * Workaround for the fact that {@code ZipFile} wasn't {@code Closeable} in earlier Java and
   * Android versions, so that we can use it with {@link Closer}.
   */
  private static final class CloseableZipFile implements Closeable {
    final ZipFile zipFile;

    CloseableZipFile(String file) throws IOException {
      this.zipFile = new ZipFile(file);
    }

    @Override
    public void close() throws IOException {
      zipFile.close();
    }
  }
}
