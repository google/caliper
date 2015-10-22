/*
 * Copyright (C) 2014 Google Inc.
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

import com.google.caliper.model.Run;
import com.google.caliper.options.CaliperOptions;
import com.google.common.base.Charsets;
import com.google.common.io.Files;
import com.google.common.util.concurrent.AbstractIdleService;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.util.LinkedHashSet;
import java.util.Set;

import javax.annotation.concurrent.GuardedBy;
import javax.inject.Inject;
import javax.inject.Singleton;

/**
 * A {@link TrialOutputFactory} implemented as a service that manages a directory either under 
 * {@code /tmp} or in a user configured directory.
 * 
 * <p>If there is a user configured directory, then no files will be deleted on service shutdown.
 * Otherwise the only way to ensure that the log files survive service shutdown is to explicitly
 * call {@link #persistFile(File)} with each file that should not be deleted.
 */
@Singleton
final class TrialOutputFactoryService
    extends AbstractIdleService implements TrialOutputFactory {
  private static final String LOG_DIRECTORY_PROPERTY = "worker.output";

  private final CaliperOptions options;
  private final Run run;

  @GuardedBy("this")
  private final Set<String> toDelete = new LinkedHashSet<String>();

  @GuardedBy("this")
  private File directory;

  @GuardedBy("this")
  private boolean persistFiles;

  @Inject TrialOutputFactoryService(Run run, CaliperOptions options) {
    this.run = run;
    this.options = options;
  }

  /** Returns the file to write trial output to. */
  @Override public FileAndWriter getTrialOutputFile(int trialNumber) throws FileNotFoundException {
    File dir;
    synchronized (this) {
      if (directory == null) {
        throw new RuntimeException(
            String.format("The output manager %s has not been started yet", this));
      }
      dir = directory;
    }
    File trialFile = new File(dir, String.format("trial-%d.log", trialNumber));
    synchronized (this) {
      if (!persistFiles) {
          toDelete.add(trialFile.getPath());
      }
    }
    return new FileAndWriter(trialFile,
        new PrintWriter(
            new BufferedWriter(
                new OutputStreamWriter(
                    new FileOutputStream(trialFile), 
                Charsets.UTF_8))));
  }

  /** 
   * Ensures that the given file will not be deleted on exit of the JVM, possibly by copying to a 
   * new file.
   */
  @Override public synchronized void persistFile(File f) {
    if (!persistFiles) {
      checkArgument(toDelete.remove(f.getPath()), "%s was not created by the output manager", f);
    }
  }

  @Override protected synchronized void startUp() throws Exception {
    File directory;
    String dirName = options.configProperties().get(LOG_DIRECTORY_PROPERTY);
    boolean persistFiles = true;
    if (dirName != null) {
      directory = new File(dirName);
      if (!directory.exists()) {
        if (!directory.mkdirs()) {
          throw new Exception(
              String.format("Unable to create directory %s indicated by property %s",
                  dirName, LOG_DIRECTORY_PROPERTY));
        }
      } else if (!directory.isDirectory()) {
        throw new Exception(
            String.format("Configured directory %s indicated by property %s is not a directory",
                dirName, LOG_DIRECTORY_PROPERTY));
      }
      // The directory exists and is a directory
      directory = new File(directory, String.format("run-%s", run.id()));
      if (!directory.mkdir()) {
        throw new Exception("Unable to create a run directory " + directory);
      }
    } else {
      // If none is configured then we don't care, just make a temp dir
      // TODO(lukes): it would be nice to use jdk7 java.nio.file.Files.createTempDir() which allows
      // us to specify a name, but caliper is still on jdk6.
      directory = Files.createTempDir();
      persistFiles = false;
    }
    this.directory = directory;
    this.persistFiles = persistFiles;
  }

  @Override protected synchronized void shutDown() throws Exception {
    if (!persistFiles) {
      // This is best effort, files to be deleted are already in a tmp directory.
      for (String f : toDelete) {
        new File(f).delete();
      }
      // This will only succeed if the directory is empty which is what we want.
      directory.delete();
    }
  }
}
