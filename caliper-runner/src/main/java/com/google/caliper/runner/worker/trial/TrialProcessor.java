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

package com.google.caliper.runner.worker.trial;

import com.google.caliper.bridge.LogMessage;
import com.google.caliper.bridge.ShouldContinueMessage;
import com.google.caliper.bridge.StopMeasurementLogMessage;
import com.google.caliper.runner.instrument.MeasurementCollectingVisitor;
import com.google.caliper.runner.options.CaliperOptions;
import com.google.caliper.runner.worker.FailureLogMessageVisitor;
import com.google.caliper.runner.worker.Worker;
import com.google.caliper.runner.worker.WorkerException;
import com.google.caliper.runner.worker.WorkerProcessor;
import com.google.caliper.util.ShortDuration;
import java.io.IOException;
import javax.annotation.Nullable;
import javax.inject.Inject;

/**
 * {@link WorkerProcessor} for building a {@link TrialResult} from data received from a worker
 * running a trial.
 *
 * @author Colin Decker
 */
final class TrialProcessor extends WorkerProcessor<TrialResult> {

  private final CaliperOptions options;
  private final TrialResultFactory trialFactory;

  // TODO(lukes): The VmDataCollectingVisitor should be able to tell us when it has collected all
  // its data.
  private final VmDataCollectingVisitor dataCollectingVisitor;
  private final MeasurementCollectingVisitor measurementCollectingVisitor;

  @Inject
  TrialProcessor(
      MeasurementCollectingVisitor measurementCollectingVisitor,
      CaliperOptions options,
      TrialResultFactory trialFactory,
      VmDataCollectingVisitor dataCollectingVisitor) {
    this.options = options;
    this.trialFactory = trialFactory;
    this.measurementCollectingVisitor = measurementCollectingVisitor;
    this.dataCollectingVisitor = dataCollectingVisitor;
  }

  @Override
  public ShortDuration timeLimit() {
    return options.timeLimit();
  }

  @Override
  public boolean handleMessage(LogMessage message, Worker worker) throws IOException {
    message.accept(FailureLogMessageVisitor.INSTANCE);
    message.accept(measurementCollectingVisitor);
    message.accept(dataCollectingVisitor);

    boolean doneCollecting = measurementCollectingVisitor.isDoneCollecting();

    // If it is a stop measurement message we need to tell the worker to either stop or keep
    // going with a WorkerContinueMessage.  This needs to be done after the
    // measurementCollecting visitor sees the message so that isDoneCollection will be up to
    // date.
    if (message instanceof StopMeasurementLogMessage) {
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

    return doneCollecting;
  }

  @Override
  public String getTimeoutErrorMessage(Worker worker) {
    return super.getTimeoutErrorMessage(worker)
        + String.format(
            " The limit (%s) may be adjusted using the --time-limit flag.", timeLimit());
  }

  @Override
  public WorkerException newWorkerException(String message, @Nullable Throwable cause) {
    return new TrialFailureException(message, cause);
  }

  @Override
  public TrialResult getResult() {
    return trialFactory.newTrialResult(dataCollectingVisitor, measurementCollectingVisitor);
  }
}
