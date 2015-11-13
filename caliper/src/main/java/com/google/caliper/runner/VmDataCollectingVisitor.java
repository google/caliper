/*
 * Copyright (C) 2013 Google Inc.
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

import com.google.caliper.bridge.AbstractLogMessageVisitor;
import com.google.caliper.bridge.FailureLogMessage;
import com.google.caliper.bridge.VmOptionLogMessage;
import com.google.caliper.bridge.VmPropertiesLogMessage;
import com.google.caliper.model.VmSpec;
import com.google.caliper.platform.Platform;
import com.google.common.base.Optional;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;

import javax.inject.Inject;

/** An {@link AbstractLogMessageVisitor} that collects data about JVM properties and options. */
@TrialScoped
final class VmDataCollectingVisitor extends AbstractLogMessageVisitor {
  private final ImmutableMap.Builder<String, String> vmOptionsBuilder = ImmutableMap.builder();
  private final Platform platform;
  private Optional<ImmutableMap<String, String>> vmProperties = Optional.absent();

  @Inject VmDataCollectingVisitor(Platform platform) {
    this.platform = platform;
  }

  /**
   * Returns a {@link VmSpec} based on the data gathered by this visitor.
   *
   * @throws IllegalStateException if not all the data has been gathered.
   */
  VmSpec vmSpec() {
    ImmutableMap<String, String> options = vmOptionsBuilder.build();
    platform.checkVmProperties(options);
    return new VmSpec.Builder()
        .addAllProperties(vmProperties.get())
        .addAllOptions(options)
        .build();
  }

  @Override
  public void visit(FailureLogMessage logMessage) {
    throw new ProxyWorkerException(logMessage.stackTrace());
  }

  @Override
  public void visit(VmOptionLogMessage logMessage) {
    vmOptionsBuilder.put(logMessage.name(), logMessage.value());
  }

  @Override
  public void visit(VmPropertiesLogMessage logMessage) {
    vmProperties = Optional.of(ImmutableMap.copyOf(
        Maps.filterKeys(logMessage.properties(), platform.vmPropertiesToRetain())));
  }
}
