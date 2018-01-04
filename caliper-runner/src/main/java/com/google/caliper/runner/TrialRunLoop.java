/*
 * Copyright (C) 2011 Google Inc.
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

import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static java.util.concurrent.TimeUnit.NANOSECONDS;

import com.google.caliper.bridge.LogMessage;
import com.google.caliper.bridge.ShouldContinueMessage;
import com.google.caliper.bridge.StopMeasurementLogMessage;
import com.google.caliper.model.Trial;
import com.google.caliper.runner.Instrument.MeasurementCollectingVisitor;
import com.google.caliper.runner.Worker.StreamItem;
import com.google.caliper.runner.options.CaliperOptions;
import com.google.caliper.util.ShortDuration;
import com.google.common.base.Stopwatch;
import com.google.common.base.Throwables;
import com.google.common.util.concurrent.Service.State;
import java.io.IOException;
import java.util.concurrent.Callable;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.inject.Inject;
import org.joda.time.Duration;

/**
 * The main data gather control loop for a Trial.
 *
 * <p>This class starts the worker process, reads all the data from it and constructs the {@link
 * Trial} while enforcing the trial timeout.
 */
@TrialScoped
class TrialRunLoop implements Callable<TrialResult> {
  private static final Logger logger = Logger.getLogger(TrialRunLoop.class.getName());

  /** The time that the worker has to clean up after an experiment. */
  private static final Duration WORKER_CLEANUP_DURATION = Duration.standardSeconds(2);

  private final CaliperOptions options;
  private final Worker worker;
  private final TrialResultFactory trialFactory;

  // TODO(lukes): The VmDataCollectingVisitor should be able to tell us when it has collected all
  // its data.
  private final VmDataCollectingVisitor dataCollectingVisitor;
  private final Stopwatch trialStopwatch = Stopwatch.createUnstarted();
  private final MeasurementCollectingVisitor measurementCollectingVisitor;
  private final WorkerOutputLogger workerOutput;

  @Inject
  TrialRunLoop(
      MeasurementCollectingVisitor measurementCollectingVisitor,
      CaliperOptions options,
      TrialResultFactory trialFactory,
      Worker worker,
      VmDataCollectingVisitor dataCollectingVisitor) {
    this.options = options;
    this.trialFactory = trialFactory;
    this.worker = worker;
    this.measurementCollectingVisitor = measurementCollectingVisitor;
    this.workerOutput = worker.outputLogger();
    this.dataCollectingVisitor = dataCollectingVisitor;
  }

  @Override
  public TrialResult call() throws TrialFailureException, IOException {
    if (worker.state() != State.NEW) {
      throw new IllegalStateException("You can only invoke the run loop once");
    }
    workerOutput.open();
    workerOutput.printHeader();
    worker.startAsync().awaitRunning();
    try {
      long timeLimitNanos = getTrialTimeLimitTrialNanos();
      boolean doneCollecting = false;
      boolean done = false;
      trialStopwatch.start();
      while (!done) {
        StreamItem item;
        try {
          item =
              worker.readItem(
                  timeLimitNanos - trialStopwatch.elapsed(NANOSECONDS), NANOSECONDS);
        } catch (InterruptedException e) {
          workerOutput.ensureFileIsSaved();
          // Someone has asked us to stop (via Futures.cancel?).
          if (doneCollecting) {
            logger.log(
                Level.WARNING,
                "Trial cancelled before completing normally (but after "
                    + "collecting sufficient data). Inspect {0} to see any worker output",
                workerOutput.outputFile());
            done = true;
            break;
          }
          // We were asked to stop but we didn't actually finish (the normal case).  Fail the trial.
          throw new TrialFailureException(
              String.format(
                  "Trial cancelled.  Inspect %s to see any worker output.",
                  workerOutput.outputFile()));
        }
        switch (item.kind()) {
          case DATA:
            LogMessage logMessage = item.content();
            logMessage.accept(measurementCollectingVisitor);
            logMessage.accept(dataCollectingVisitor);
            if (!doneCollecting && measurementCollectingVisitor.isDoneCollecting()) {
              doneCollecting = true;
              // We have received all the measurements we need and are about to tell the worker to
              // shut down.  At this point the worker should shutdown soon, but we don't want to
              // wait too long, so decrease the time limit so that we wait no more than
              // WORKER_CLEANUP_DURATION.
              long cleanupTimeNanos = MILLISECONDS.toNanos(WORKER_CLEANUP_DURATION.getMillis());
              // TODO(lukes): Does the min operation make sense here? should we just use the
              // cleanupTimeNanos?
              timeLimitNanos = trialStopwatch.elapsed(NANOSECONDS) + cleanupTimeNanos;
            }
            // If it is a stop measurement message we need to tell the worker to either stop or keep
            // going with a WorkerContinueMessage.  This needs to be done after the
            // measurementCollecting visitor sees the message so that isDoneCollection will be up to
            // date.
            if (logMessage instanceof StopMeasurementLogMessage) {
              // TODO(lukes): this is a blocking write, perhaps we should perform it in a non
              // blocking manner to keep this thread only blocking in one place.  This would
              // complicate error handling, but may increase performance since it would free this
              // thread up to handle other messages
              worker.sendMessage(
                  new ShouldContinueMessage(
                      !doneCollecting, measurementCollectingVisitor.isWarmupComplete()));
              if (doneCollecting) {
                worker.closeWriter();
              }
            }
            break;
          case EOF:
            // We consider EOF to be synonymous with worker shutdown
            if (!doneCollecting) {
              workerOutput.ensureFileIsSaved();
              throw new TrialFailureException(
                  String.format(
                      "The worker exited without producing "
                          + "data. It has likely crashed. Inspect %s to see any worker output.",
                      workerOutput.outputFile()));
            }
            done = true;
            break;
          case TIMEOUT:
            workerOutput.ensureFileIsSaved();
            if (doneCollecting) {
              // Should this be an error?
              logger.log(
                  Level.WARNING,
                  "Worker failed to exit cleanly within the alloted time. "
                      + "Inspect {0} to see any worker output",
                  workerOutput.outputFile());
              done = true;
            } else {
              throw new TrialFailureException(
                  String.format(
                      "Trial exceeded the total allowable runtime (%s). "
                          + "The limit may be adjusted using the --time-limit flag.  Inspect %s to "
                          + "see any worker output",
                      options.timeLimit(), workerOutput.outputFile()));
            }
            break;
          default:
            throw new AssertionError("Impossible item: " + item);
        }
      }
      return trialFactory.newTrialResult(dataCollectingVisitor, measurementCollectingVisitor);
    } catch (Throwable e) {
      Throwables.propagateIfInstanceOf(e, TrialFailureException.class);
      // This is some failure that is not a TrialFailureException, let the exception propagate but
      // log the filename for the user.
      workerOutput.ensureFileIsSaved();
      logger.severe(
          String.format(
              "Unexpected error while executing trial. Inspect %s to see any worker output.",
              workerOutput.outputFile()));
      throw Throwables.propagate(e);
    } finally {
      trialStopwatch.reset();
      worker.stopAsync();
      workerOutput.close();
    }
  }

  private long getTrialTimeLimitTrialNanos() {
    ShortDuration timeLimit = options.timeLimit();
    if (ShortDuration.zero().equals(timeLimit)) {
      return Long.MAX_VALUE;
    }
    return timeLimit.to(NANOSECONDS);
  }
}
