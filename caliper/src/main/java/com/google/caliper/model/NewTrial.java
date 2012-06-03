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

package com.google.caliper.model;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;

import com.google.common.collect.ImmutableList;

import java.util.UUID;

import javax.annotation.concurrent.Immutable;

/**
 * An invocation of a single scenario measured with a single instrument and the results thereof.
 *
 * @author gak@google.com (Gregory Kick)
 */
@Immutable
public final class NewTrial { // used to be Result
  private final UUID id;
  private final NewRun run;
  private final NewInstrumentSpec instrumentSpec;
  private final NewScenario scenario;
  private final ImmutableList<NewMeasurement> measurements;

  private NewTrial(Builder builder) {
    this.id = builder.id;
    this.run = builder.run;
    this.instrumentSpec = builder.instrumentSpec;
    this.scenario = builder.scenario;
    this.measurements = builder.measurementsBuilder.build();
  }

  public UUID id() {
    return id;
  }

  public NewRun run() {
    return run;
  }

  public NewInstrumentSpec instrumentSpec() {
    return instrumentSpec;
  }

  public NewScenario scenario() {
    return scenario;
  }

  public ImmutableList<NewMeasurement> measurements() {
    return measurements;
  }

  public static final class Builder {
    private final UUID id;
    private NewRun run;
    private NewInstrumentSpec instrumentSpec;
    private NewScenario scenario;
    private final ImmutableList.Builder<NewMeasurement> measurementsBuilder =
        ImmutableList.builder();

    public Builder(UUID id) {
      this.id = checkNotNull(id);
    }

    public Builder run(NewRun.Builder runBuilder) {
      return run(runBuilder.build());
    }

    public Builder run(NewRun run) {
      this.run = checkNotNull(run);
      return this;
    }

    public Builder instrumentSpec(NewInstrumentSpec.Builder instrumentSpecBuilder) {
      return instrumentSpec(instrumentSpecBuilder.build());
    }

    public Builder instrumentSpec(NewInstrumentSpec instrumentSpec) {
      this.instrumentSpec = checkNotNull(instrumentSpec);
      return this;
    }

    public Builder scenario(NewScenario.Builder scenarioBuilder) {
      return scenario(scenarioBuilder.build());
    }

    public Builder scenario(NewScenario scenario) {
      this.scenario = checkNotNull(scenario);
      return this;
    }

    public Builder addMeasurement(NewMeasurement.Builder measurementBuilder) {
      return addMeasurement(measurementBuilder.build());
    }

    public Builder addMeasurement(NewMeasurement measurement) {
      this.measurementsBuilder.add(measurement);
      return this;
    }

    public Builder addAllMeasurements(Iterable<NewMeasurement> measurements) {
      this.measurementsBuilder.addAll(measurements);
      return this;
    }

    public NewTrial build() {
      checkState(run != null);
      checkState(instrumentSpec != null);
      checkState(scenario != null);
      return new NewTrial(this);
    }
  }
}
