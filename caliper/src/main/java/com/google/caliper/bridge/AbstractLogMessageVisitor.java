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

package com.google.caliper.bridge;

/**
 * An abstract {@link LogMessageVisitor} with no-op implementations for the visit methods -
 * provided so that all visitors don't have to override all methods.
 */
public abstract class AbstractLogMessageVisitor implements LogMessageVisitor {
  @Override public void visit(GcLogMessage logMessage) {}

  @Override public void visit(FailureLogMessage logMessage) {}

  @Override public void visit(HotspotLogMessage logMessage) {}

  @Override public void visit(StartMeasurementLogMessage logMessage) {}

  @Override public void visit(StopMeasurementLogMessage logMessage) {}

  @Override public void visit(VmOptionLogMessage logMessage) {}

  @Override public void visit(VmPropertiesLogMessage logMessage) {}
}
