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

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;

/** 
 * A factory for trial log files.
 * 
 * <p>The log files may be configured to be deleted on exit of the runner process.  If the files
 * should not be deleted then call {@link #persistFile(File)} to ensure that they survive.
 */
interface TrialOutputFactory {
  
  /** A simple tuple of a {@link File} and a {@link PrintWriter} for writing to that file. */
  final class FileAndWriter {
    final File file;
    final PrintWriter writer;

    FileAndWriter(File file, PrintWriter writer) {
      this.file = file;
      this.writer = writer;
    }
  }

  /** Returns the file to write trial output to. */
  FileAndWriter getTrialOutputFile(int trialNumber) throws FileNotFoundException;

  /** 
   * Ensures that the given file will not be deleted after the run.  The file provided must be equal
   * to a file returned by {@link #getTrialOutputFile(int)}.
   */
  void persistFile(File f);
}
