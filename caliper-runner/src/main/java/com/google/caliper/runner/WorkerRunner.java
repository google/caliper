/*
 * Copyright (C) 2017 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */

package com.google.caliper.runner;

import static com.google.common.base.Charsets.UTF_8;
import static com.google.common.base.Preconditions.checkState;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static java.util.concurrent.TimeUnit.NANOSECONDS;

import com.google.caliper.runner.Worker.StreamItem;
import com.google.caliper.runner.options.CaliperOptions;
import com.google.common.base.Stopwatch;
import com.google.common.base.Throwables;
import com.google.common.io.Files;
import com.google.common.util.concurrent.Service.State;
import java.io.File;
import java.io.IOException;
import java.util.concurrent.Callable;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.inject.Inject;
import org.joda.time.Duration;

/**
 * {@link Callable} that starts a worker, reads data from it, processes that data with a {@link
 * WorkerProcessor}, and finally returns a result.
 *
 * @author Colin Decker
 */
@WorkerScoped
final class WorkerRunner<R> implements Callable<R> {
  private static final Logger logger = Logger.getLogger(WorkerRunner.class.getName());

  /** The time that the worker has to clean up after running. */
  private static final Duration WORKER_CLEANUP_DURATION = Duration.standardSeconds(2);

  private final Worker worker;
  private final WorkerProcessor<R> processor;
  private final boolean printWorkerLog;

  private File outputFile = null;

  private boolean doneProcessing = false;
  private boolean done = false;

  @Inject
  WorkerRunner(Worker worker, WorkerProcessor<R> processor, CaliperOptions options) {
    this.worker = worker;
    this.processor = processor;
    this.printWorkerLog = options.printWorkerLog();
  }

  /**
   * @deprecated Call {@link #runWorker} instead. This method is only provided so this class can be
   *     passed to methods accepting {@link Callable}.
   */
  @Deprecated
  @Override
  public R call() {
    return runWorker();
  }

  /**
   * Starts up the worker process and runs it to completion, processing data received from it with
   * the provided {@link WorkerProcessor}. Returns the result object produced by the processor.
   */
  public R runWorker() {
    checkState(worker.state() == State.NEW, "You can only invoke the run loop once");

    // logger must be opened before starting worker
    WorkerOutputLogger workerLogger = worker.outputLogger();
    try {
      workerLogger.open();
    } catch (IOException e) {
      throw processor.newWorkerException(
          String.format("Failed to open output logger for worker [%s].", worker.name()), e);
    }
    outputFile = workerLogger.outputFile();

    worker.startAsync();
    try {
      workerLogger.printHeader();

      long timeLimitNanos = processor.timeLimit().to(NANOSECONDS);
      Stopwatch stopwatch = Stopwatch.createUnstarted();

      worker.awaitRunning();
      worker.sendRequest();

      stopwatch.start();
      while (!done) {
        StreamItem item;
        try {
          item = worker.readItem(timeLimitNanos - stopwatch.elapsed(NANOSECONDS), NANOSECONDS);
        } catch (InterruptedException e) {
          // Someone has asked us to stop (via Futures.cancel?).
          if (!doneProcessing) {
            throw processor.newWorkerException(
                formatError(processor.getInterruptionErrorMessage(worker)), e);
          }
          logger.log(
              Level.WARNING,
              // Yes, we're doing the formatting eagerly here even though the log level might not
              // be enabled. It seems like a small sacrifice in this case for more readable code.
              formatError(
                  "Worker [%s] cancelled before completing normally, but after getting results.",
                  worker));
          done = true;
          break;
        }

        switch (item.kind()) {
          case DATA:
            doneProcessing = processor.handleMessage(item.content(), worker);
            if (doneProcessing) {
              // The worker should be done now; give it WORKER_CLEANUP_DURATION nanos to finish
              // shutting down.
              long cleanupTimeNanos = MILLISECONDS.toNanos(WORKER_CLEANUP_DURATION.getMillis());
              timeLimitNanos = stopwatch.elapsed(NANOSECONDS) + cleanupTimeNanos;
            }
            break;
          case EOF:
            // We consider EOF to be synonymous with worker shutdown
            if (!doneProcessing) {
              throw processor.newWorkerException(
                  formatError(processor.getPrematureExitErrorMessage(worker)), null);
            }
            done = true;
            break;
          case TIMEOUT:
            if (!doneProcessing) {
              throw processor.newWorkerException(
                  formatError(processor.getTimeoutErrorMessage(worker)), null);
            }
            logger.log(
                Level.WARNING,
                formatError("Worker [%s] failed to exit cleanly within the alloted time.", worker));
            done = true;
            break;
          default:
            throw new AssertionError("Impossible item: " + item);
        }
      }

      return processor.getResult();
    } catch (WorkerException e) {
      throw e;
    } catch (Throwable e) {
      logger.severe(formatError("Unexpected error while running worker [%s].", worker));
      Throwables.throwIfUnchecked(e);
      throw new RuntimeException(e);
    } finally {
      worker.stopAsync();
      try {
        workerLogger.ensureFileIsSaved();
      } finally {
        workerLogger.close();
      }
    }
  }

  /**
   * Formats an error message for throwing or logging, adding the path to the worker output file to
   * the message. The given {@code baseMessageFormat} must include a "%s" to allow the worker name
   * to be added.
   */
  private String formatError(String baseMessageFormat, Object... args) {
    String baseMessage = String.format(baseMessageFormat, args);
    if (printWorkerLog) {
      try {
        worker.outputLogger().flush();
        String logContent = Files.toString(outputFile, UTF_8);
        return baseMessage + " Worker log follows:\n\n" + logContent;
      } catch (IOException ignore) {
        // fall through to printing the path
      }
    }

    return baseMessage + String.format(" Inspect %s to see any worker output.", outputFile);
  }
}
