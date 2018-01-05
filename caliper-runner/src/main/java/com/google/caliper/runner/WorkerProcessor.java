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

package com.google.caliper.runner;

import com.google.caliper.bridge.LogMessage;
import com.google.caliper.util.ShortDuration;
import java.io.IOException;
import javax.annotation.Nullable;

/**
 * Processor for interacting with a {@link Worker}.
 *
 * @author Colin Decker
 */
abstract class WorkerProcessor<R> {

  /** Returns the amount of time that the worker being processed should be allowed to run. */
  public abstract ShortDuration timeLimit();

  /**
   * Called when a {@code message} is received from the given {@code worker}. Returns {@code true}
   * if the processor is done processing and can produce a result; {@code false} otherwise.
   */
  public abstract boolean handleMessage(LogMessage message, Worker worker) throws IOException;

  /**
   * Returns the base error message to use when the worker runner's thread is interrupted before
   * this processor has finished processing.
   */
  public String getInterruptionErrorMessage(Worker worker) {
    return String.format("Worker [%s] cancelled.", worker.name());
  }

  /**
   * Returns the base error message to use when the worker exits/dies before this processor has
   * finished processing.
   */
  public String getPrematureExitErrorMessage(Worker worker) {
    return String.format(
        "Worker [%s] exited without producing data. It has likely crashed.", worker.name());
  }

  /**
   * Returns the base error message to use when the worker exceeds the {@link #timeLimit} before
   * this processor has finished processing.
   */
  public String getTimeoutErrorMessage(Worker worker) {
    return String.format("Worker [%s] exceeded the total allowable runtime.", worker.name());
  }

  /**
   * Creates a new {@link WorkerException} to be thrown in the case of worker failure, allowing a
   * more specific subtype to be thrown.
   */
  public WorkerException newWorkerException(String message, @Nullable Throwable cause) {
    return new WorkerException(message, cause);
  }

  /** Returns the result produced by this processor. */
  public abstract R getResult();
}
