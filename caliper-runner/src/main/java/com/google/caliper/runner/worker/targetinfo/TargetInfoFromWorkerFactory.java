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

import com.google.caliper.bridge.TargetInfoLogMessage;
import com.google.caliper.core.BenchmarkClassModel;
import com.google.caliper.core.InvalidBenchmarkException;
import com.google.caliper.core.UserCodeException;
import com.google.caliper.model.Host;
import com.google.caliper.runner.config.InvalidConfigurationException;
import com.google.caliper.runner.target.Target;
import com.google.caliper.runner.worker.ProxyWorkerException;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import com.google.common.collect.SetMultimap;
import java.util.HashMap;
import java.util.Map;
import javax.inject.Inject;
import javax.inject.Provider;

/**
 * {@link TargetInfoFactory} implementation that creates a worker for each target and queries it for
 * its info.
 *
 * <p>The expectation is that all targets should produce identical models of the benchmark class. If
 * they do not, that means different targets are seeing different definitions of the class that
 * can't be compared directly, so {@link InvalidConfigurationException} is thrown. If all models are
 * the same, that single model is returned.
 *
 * @author Colin Decker
 */
public final class TargetInfoFromWorkerFactory implements TargetInfoFactory {

  private final ImmutableSet<Target> targets;
  private final Provider<TargetInfoComponent.Builder> targetInfoComponentBuilder;

  @Inject
  TargetInfoFromWorkerFactory(
      ImmutableSet<Target> targets,
      Provider<TargetInfoComponent.Builder> targetInfoComponentBuilder) {
    this.targets = targets;
    this.targetInfoComponentBuilder = targetInfoComponentBuilder;
  }

  // TODO(cgdecker): What we may actually want to do once different types of targets (e.g. JVM
  // target and Android target) are supported is to just pick one target of each type since worker
  // classpaths should only differ by type, not individual target.

  @Override
  public TargetInfo getTargetInfo() {
    SetMultimap<BenchmarkClassModel, Target> models = HashMultimap.create();
    Map<Target, Host> hosts = new HashMap<>();
    try {
      for (Target target : targets) {
        TargetInfoLogMessage logMessage =
            targetInfoComponentBuilder.get().target(target).build().workerRunner().runWorker();
        models.put(logMessage.model(), target);
        hosts.put(
            target, new Host.Builder().addAllProperties(logMessage.deviceProperties()).build());
      }
    } catch (ProxyWorkerException e) {
      if (e.exceptionType().equals(UserCodeException.class.getName())) {
        throw new UserCodeException(e.message(), e);
      } else if (e.exceptionType().equals(InvalidBenchmarkException.class.getName())) {
        throw new InvalidBenchmarkException(e.message(), e);
      }
      throw e;
    }

    if (models.keySet().size() > 1) {
      throw new InvalidConfigurationException(
          "Different targets produced different models of the benchmark class. Please ensure "
              + "that the classpaths used for each type of target contain equivalent versions of "
              + "the benchmark class.");
    }

    return TargetInfo.create(Iterables.getOnlyElement(models.keySet()), hosts);
  }
}
