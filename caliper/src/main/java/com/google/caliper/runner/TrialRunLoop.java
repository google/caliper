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
import com.google.caliper.options.CaliperOptions;
import com.google.caliper.runner.Instrument.MeasurementCollectingVisitor;
import com.google.caliper.runner.StreamService.StreamItem;
import com.google.caliper.util.ShortDuration;
import com.google.common.base.Stopwatch;
import com.google.common.util.concurrent.Service.State;
import com.google.gson.Gson;
import com.google.inject.Inject;

import org.joda.time.Duration;

import java.io.IOException;
import java.util.concurrent.Callable;
import java.util.logging.Logger;

/**
 * The main data gather control loop for a Trial.
 *
 * <p>This class starts the worker process, reads all the data from it and constructs the
 * {@link Trial} while enforcing the trial timeout.
 */
@TrialScoped class TrialRunLoop implements Callable<Trial> {
  private static final Logger logger = Logger.getLogger(TrialRunLoop.class.getName());

  /** The time that the worker has to clean up after an experiment. */
  private static final Duration WORKER_CLEANUP_DURATION = Duration.standardSeconds(2);

  private final CaliperOptions options;
  private final StreamService streamService;
  private final Gson gson;
  private final TrialFactory trialFactory;

  // TODO(lukes): The VmDataCollectingVisitor should be able to tell us when it has collected all
  // its data.
  private final VmDataCollectingVisitor dataCollectingVisitor = new VmDataCollectingVisitor();
  private final Stopwatch trialStopwatch = new Stopwatch();
  private final MeasurementCollectingVisitor measurementCollectingVisitor;

  @Inject TrialRunLoop(
      MeasurementCollectingVisitor measurementCollectingVisitor,
      CaliperOptions options,
      TrialFactory trialFactory,
      Gson gson,
      StreamService streamService) {
    this.options = options;
    this.trialFactory = trialFactory;
    this.gson = gson;
    this.streamService = streamService;
    this.measurementCollectingVisitor = measurementCollectingVisitor; 
  }

  @Override public Trial call() throws TrialFailureException, IOException {
    if (streamService.state() != State.NEW) {
      throw new IllegalStateException("You can only invoke the run loop once");
    }
    streamService.startAndWait();
    try {
      long timeLimitNanos = getTrialTimeLimitTrialNanos();
      boolean doneCollecting = false;
      boolean done = false;
      while (!done) {
        StreamItem item = streamService.readItem(
            timeLimitNanos - trialStopwatch.elapsed(NANOSECONDS), 
            NANOSECONDS);
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
              streamService.writeLine(gson.toJson(new ShouldContinueMessage(!doneCollecting)));
              if (doneCollecting) {
                streamService.closeWriter();
              }
            }
            break;
          case EOF:
            // We consider EOF to be synonymous with worker shutdown
            if (!doneCollecting) {
              throw new TrialFailureException("The worker exited without producing data. It has "
                  + "likely crashed. Run with --verbose to see any worker output.");
            }
            done = true;
            break;
          case TIMEOUT:
            if (doneCollecting) {
              // Should this be an error?
              logger.warning("Worker failed to exit cleanly within the alloted time.");  
              done = true;
            } else {
              throw new TrialFailureException(String.format(
                  "Trial exceeded the total allowable runtime (%s). "
                      + "The limit may be adjusted using the --time-limit flag.",
                      options.timeLimit()));
            }
            break;
          default:
            throw new AssertionError("Impossible item: " + item);
        }
      }
      return trialFactory.newTrial(dataCollectingVisitor, measurementCollectingVisitor);
    } catch (InterruptedException e) {
      throw new AssertionError();
    } finally {
      trialStopwatch.reset();
      streamService.stop();
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
