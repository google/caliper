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

import static com.google.common.base.Preconditions.checkState;

import com.google.caliper.runner.WorkerOutputFactory.FileAndWriter;
import java.io.Closeable;
import java.io.File;
import java.io.Flushable;
import java.io.IOException;
import java.io.PrintWriter;
import javax.annotation.concurrent.GuardedBy;
import javax.inject.Inject;

/** A logger to write worker output to a file. */
@WorkerScoped
final class WorkerOutputLogger implements Flushable, Closeable {
  @GuardedBy("this")
  private File file;

  @GuardedBy("this")
  private PrintWriter writer;

  private final WorkerOutputFactory outputManager;
  private final WorkerSpec workerSpec;
  private final Command command;

  @Inject
  WorkerOutputLogger(WorkerOutputFactory outputManager, WorkerSpec workerSpec, Command command) {
    this.outputManager = outputManager;
    this.workerSpec = workerSpec;
    this.command = command;
  }

  /** Opens the trial output file. */
  synchronized void open() throws IOException {
    if (writer == null) {
      FileAndWriter fileAndWriter = outputManager.getOutputFile(workerSpec.name() + ".log");
      file = fileAndWriter.file;
      writer = fileAndWriter.writer;
    }
  }

  /**
   * Ensures that the writer has been opened. also creates a happens-before edge that ensures that
   * writer is visible (and non-null) after a non-exceptional return from this method.
   */
  private synchronized void checkOpened() {
    checkState(writer != null, "The logger is not open");
  }

  /** Prints header information to the file. */
  synchronized void printHeader() {
    checkOpened();
    // make the file self describing
    writer.println("Command: " + command.argumentsString());
    writer.println();
    workerSpec.printInfoHeader(writer);
    writer.println();
  }

  /**
   * Logs a line of output to the logger.
   *
   * @param source The source of the line (e.g. 'stderr')
   * @param line The output
   */
  synchronized void log(String source, String line) {
    checkOpened();
    writer.printf("[%s] %s%n", source, line);
  }

  @Override
  public synchronized void flush() {
    if (writer != null) {
      writer.flush();
    }
  }

  @Override
  public synchronized void close() {
    if (writer != null) {
      writer.close();
    }
  }

  /** Marks the log file so that it will not be deleted at the end of the benchmark. */
  synchronized void ensureFileIsSaved() {
    checkOpened();
    outputManager.persistFile(file);
  }

  /** Returns the log file path. */
  synchronized File outputFile() {
    checkOpened();
    return file;
  }
}
