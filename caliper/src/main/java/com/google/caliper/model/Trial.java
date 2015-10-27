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
import static javax.persistence.AccessType.FIELD;
import static javax.persistence.CascadeType.MERGE;
import static javax.persistence.CascadeType.PERSIST;

import com.google.common.base.MoreObjects;
import com.google.common.base.Objects;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;

import org.hibernate.annotations.Immutable;
import org.hibernate.annotations.Type;

import java.util.List;
import java.util.UUID;

import javax.persistence.Access;
import javax.persistence.Cacheable;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import javax.persistence.NamedQuery;
import javax.persistence.OneToMany;
import javax.persistence.OrderColumn;
import javax.persistence.QueryHint;

/**
 * An invocation of a single scenario measured with a single instrument and the results thereof.
 *
 * @author gak@google.com (Gregory Kick)
 */
@Entity
@Access(FIELD)
@Immutable
@Cacheable
@NamedQuery(
    name = "getTrialsForRuns",
    query = "SELECT t FROM Trial t WHERE t.run.id IN :runIds",
    hints = @QueryHint(name = "org.hibernate.readOnly", value = "true"))
public final class Trial { // used to be Result
  public static final Trial DEFAULT = new Trial();

  @Id
  @Type(type = "uuid-binary")
  @Column(length = 16)
  private UUID id;
  @ManyToOne(optional = false)
  private Run run;
  @ManyToOne(optional = false)
  private InstrumentSpec instrumentSpec;
  @ManyToOne(optional = false)
  private Scenario scenario;
  @OneToMany(cascade = {MERGE, PERSIST})
  @OrderColumn(name = "id") // because hibernate breaks hashCode otherwise
  private List<Measurement> measurements;

  private Trial() {
    this.id = Defaults.UUID;
    this.run = Run.DEFAULT;
    this.instrumentSpec = InstrumentSpec.DEFAULT;
    this.scenario = Scenario.DEFAULT;
    this.measurements = Lists.newArrayList();
  }

  private Trial(Builder builder) {
    this.id = builder.id;
    this.run = builder.run;
    this.instrumentSpec = builder.instrumentSpec;
    this.scenario = builder.scenario;
    this.measurements = Lists.newArrayList(builder.measurements);
  }

  public UUID id() {
    return id;
  }

  public Run run() {
    return run;
  }

  public InstrumentSpec instrumentSpec() {
    return instrumentSpec;
  }

  public Scenario scenario() {
    return scenario;
  }

  public ImmutableList<Measurement> measurements() {
    return ImmutableList.copyOf(measurements);
  }

  @Override public boolean equals(Object obj) {
    if (obj == this) {
      return true;
    } else if (obj instanceof Trial) {
      Trial that = (Trial) obj;
      return this.id.equals(that.id)
          && this.run.equals(that.run)
          && this.instrumentSpec.equals(that.instrumentSpec)
          && this.scenario.equals(that.scenario)
          && this.measurements.equals(that.measurements);
    } else {
      return false;
    }
  }

  @Override public int hashCode() {
    return Objects.hashCode(id, run, instrumentSpec, scenario, measurements);
  }

  @Override public String toString() {
    return MoreObjects.toStringHelper(this)
        .add("id", id)
        .add("run", run)
        .add("instrumentSpec", instrumentSpec)
        .add("scenario", scenario)
        .add("measurements", measurements)
        .toString();
  }

  public static final class Builder {
    private final UUID id;
    private Run run;
    private InstrumentSpec instrumentSpec;
    private Scenario scenario;
    private final List<Measurement> measurements = Lists.newArrayList();

    public Builder(UUID id) {
      this.id = checkNotNull(id);
    }

    public Builder run(Run.Builder runBuilder) {
      return run(runBuilder.build());
    }

    public Builder run(Run run) {
      this.run = checkNotNull(run);
      return this;
    }

    public Builder instrumentSpec(InstrumentSpec.Builder instrumentSpecBuilder) {
      return instrumentSpec(instrumentSpecBuilder.build());
    }

    public Builder instrumentSpec(InstrumentSpec instrumentSpec) {
      this.instrumentSpec = checkNotNull(instrumentSpec);
      return this;
    }

    public Builder scenario(Scenario.Builder scenarioBuilder) {
      return scenario(scenarioBuilder.build());
    }

    public Builder scenario(Scenario scenario) {
      this.scenario = checkNotNull(scenario);
      return this;
    }

    public Builder addMeasurement(Measurement.Builder measurementBuilder) {
      return addMeasurement(measurementBuilder.build());
    }

    public Builder addMeasurement(Measurement measurement) {
      this.measurements.add(measurement);
      return this;
    }

    public Builder addAllMeasurements(Iterable<Measurement> measurements) {
      Iterables.addAll(this.measurements, measurements);
      return this;
    }

    public Trial build() {
      checkState(run != null);
      checkState(instrumentSpec != null);
      checkState(scenario != null);
      return new Trial(this);
    }
  }
}
