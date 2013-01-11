/*
 * Copyright (C) 2012 Google Inc.
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

import com.google.common.io.Files;

import java.io.File;
import java.io.IOException;

/**
 * A utility for working with <a href="http://en.wikipedia.org/wiki/Named_pipe">named pipes</a>.
 */
public final class Pipes {
  private Pipes() {}

  public static File createPipe() throws IOException {
    File parentDirectory = Files.createTempDir();
    parentDirectory.deleteOnExit();
    File pipeFile = new File(parentDirectory, "pipe");
    pipeFile.deleteOnExit();
    ProcessBuilder mkfifoProcessBuilder = new ProcessBuilder("mkfifo", pipeFile.getAbsolutePath());
    try {
      Process process = mkfifoProcessBuilder.start();
      int exitValue = process.waitFor();
      if (exitValue != 0) {
        cleanUpAndFail(pipeFile);
      }
    } catch (IOException e) {
      cleanUpAndFail(pipeFile);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      cleanUpAndFail(pipeFile);
    }
    // double check
    if (!pipeFile.exists()) {
      cleanUpAndFail(pipeFile);
    }
    return pipeFile;
  }

  private static void cleanUpAndFail(File pipeFile) throws IOException {
    if (pipeFile.exists()) {
      pipeFile.delete();
    }
    File parentDirectory = pipeFile.getParentFile();
    if (parentDirectory.exists()) {
      parentDirectory.delete();
    }
    throw new IOException("could not create pipe: " + pipeFile);
  }
}
