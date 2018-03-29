/*
 * Copyright (C) 2018 Google Inc.
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

package com.google.caliper.runner.worker.targetinfo;

import static java.util.concurrent.TimeUnit.MINUTES;

import com.google.caliper.bridge.AbstractLogMessageVisitor;
import com.google.caliper.bridge.LogMessage;
import com.google.caliper.bridge.LogMessageVisitor;
import com.google.caliper.bridge.TargetInfoLogMessage;
import com.google.caliper.runner.worker.FailureLogMessageVisitor;
import com.google.caliper.runner.worker.Worker;
import com.google.caliper.runner.worker.WorkerProcessor;
import com.google.caliper.util.ShortDuration;
import javax.annotation.Nullable;
import javax.inject.Inject;

/** {@link WorkerProcessor} for receiving a target info from the worker. */
final class TargetInfoProcessor extends WorkerProcessor<TargetInfoLogMessage> {

  @Nullable private volatile TargetInfoLogMessage result = null;

  private final LogMessageVisitor successVisitor =
      new AbstractLogMessageVisitor() {
        @Override
        public void visit(TargetInfoLogMessage logMessage) {
          TargetInfoProcessor.this.result = logMessage;
        }
      };

  @Inject
  TargetInfoProcessor() {}

  @Override
  public ShortDuration timeLimit() {
    // Should not take nearly this long.
    return ShortDuration.of(5, MINUTES);
  }

  @Override
  public boolean handleMessage(LogMessage message, Worker worker) {
    message.accept(FailureLogMessageVisitor.INSTANCE);
    message.accept(successVisitor);
    return result != null;
  }

  @Override
  public TargetInfoLogMessage getResult() {
    return result;
  }
}
