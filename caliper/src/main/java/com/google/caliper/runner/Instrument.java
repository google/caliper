/*
 * Copyright (C) 2011 Google Inc.
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

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

import com.google.caliper.bridge.AbstractLogMessageVisitor;
import com.google.caliper.bridge.LogMessageVisitor;
import com.google.caliper.bridge.StopMeasurementLogMessage;
import com.google.caliper.config.VmConfig;
import com.google.caliper.model.InstrumentSpec;
import com.google.caliper.model.Measurement;
import com.google.caliper.worker.Worker;
import com.google.common.base.MoreObjects;
import com.google.common.base.Objects;
import com.google.common.base.Predicates;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ListMultimap;
import com.google.common.collect.Maps;

import java.lang.reflect.Method;

import javax.inject.Inject;

public abstract class Instrument {
  protected ImmutableMap<String, String> options;
  private String name = getClass().getSimpleName();

  @Inject void setOptions(@InstrumentOptions ImmutableMap<String, String> options) {
    this.options = ImmutableMap.copyOf(
        Maps.filterKeys(options, Predicates.in(instrumentOptions())));
  }

  @Inject void setInstrumentName(@InstrumentName String name) {
    this.name = name;
  }

  String name() {
    return name;
  }

  @Override public String toString() {
    return name();
  }

  public abstract boolean isBenchmarkMethod(Method method);

  public abstract Instrumentation createInstrumentation(Method benchmarkMethod)
      throws InvalidBenchmarkException;

  /**
   * Indicates that trials using this instrument can be run in parallel with other trials.
   */
  public abstract TrialSchedulingPolicy schedulingPolicy();

  /**
   * The application of an instrument to a particular benchmark method.
   */
  // TODO(gak): consider passing in Instrument explicitly for DI
  public abstract class Instrumentation {
    protected Method benchmarkMethod;

    protected Instrumentation(Method benchmarkMethod) {
      this.benchmarkMethod = checkNotNull(benchmarkMethod);
    }

    Instrument instrument() {
      return Instrument.this;
    }

    Method benchmarkMethod() {
      return benchmarkMethod;
    }

    @Override
    public final boolean equals(Object obj) {
      if (obj == this) {
        return true;
      } else if (obj instanceof Instrumentation) {
        Instrumentation that = (Instrumentation) obj;
        return Instrument.this.equals(that.instrument())
            && this.benchmarkMethod.equals(that.benchmarkMethod);
      }
      return super.equals(obj);
    }

    @Override
    public final int hashCode() {
      return Objects.hashCode(Instrument.this, benchmarkMethod);
    }

    @Override
    public String toString() {
      return MoreObjects.toStringHelper(Instrumentation.class)
          .add("instrument", Instrument.this)
          .add("benchmarkMethod", benchmarkMethod)
          .toString();
    }

    public abstract void dryRun(Object benchmark) throws InvalidBenchmarkException;

    public abstract Class<? extends Worker> workerClass();

    /**
     * Return the subset of options (and possibly a transformation thereof) to be used in the
     * worker. Returns all instrument options by default.
     */
    public ImmutableMap<String, String> workerOptions() {
      return options;
    }

    abstract MeasurementCollectingVisitor getMeasurementCollectingVisitor();
  }

  public final ImmutableMap<String, String> options() {
    return options;
  }

  final InstrumentSpec getSpec() {
    return new InstrumentSpec.Builder()
        .instrumentClass(getClass())
        .addAllOptions(options())
        .build();
  }

  /**
   * Defines the list of options applicable to this instrument. Implementations that use options
   * will need to override this method.
   */
  protected ImmutableSet<String> instrumentOptions() {
    return ImmutableSet.of();
  }

  /**
   * Returns some arguments that should be added to the command line when invoking
   * this instrument's worker.
   *
   * @param vmConfig the configuration for the VM on which this is running.
   */
  ImmutableSet<String> getExtraCommandLineArgs(VmConfig vmConfig) {
    return vmConfig.commonInstrumentVmArgs();
  }

  interface MeasurementCollectingVisitor extends LogMessageVisitor {
    boolean isDoneCollecting();
    boolean isWarmupComplete();
    ImmutableList<Measurement> getMeasurements();
    /**
     * Returns all the messages created while collecting measurments.
     * 
     * <p>A message is some piece of user visible data that should be displayed to the user along
     * with the trial results.
     * 
     * <p>TODO(lukes): should we model these as anything more than strings.  These messages already
     * have a concept of 'level' based on the prefix.
     */
    ImmutableList<String> getMessages();
  }

  /**
   * A default implementation of {@link MeasurementCollectingVisitor} that collects measurements for
   * pre-specified descriptions.
   */
  protected static final class DefaultMeasurementCollectingVisitor
      extends AbstractLogMessageVisitor implements MeasurementCollectingVisitor {
    static final int DEFAULT_NUMBER_OF_MEASUREMENTS = 9;
    final ImmutableSet<String> requiredDescriptions;
    final ListMultimap<String, Measurement> measurementsByDescription;
    final int requiredMeasurements;

    DefaultMeasurementCollectingVisitor(ImmutableSet<String> requiredDescriptions) {
      this(requiredDescriptions, DEFAULT_NUMBER_OF_MEASUREMENTS);
    }

    DefaultMeasurementCollectingVisitor(ImmutableSet<String> requiredDescriptions,
        int requiredMeasurements) {
      this.requiredDescriptions = requiredDescriptions;
      checkArgument(!requiredDescriptions.isEmpty());
      this.requiredMeasurements = requiredMeasurements;
      checkArgument(requiredMeasurements > 0);
      this.measurementsByDescription =
          ArrayListMultimap.create(requiredDescriptions.size(), requiredMeasurements);
    }

    @Override public void visit(StopMeasurementLogMessage logMessage) {
      for (Measurement measurement : logMessage.measurements()) {
        measurementsByDescription.put(measurement.description(), measurement);
      }
    }

    @Override public boolean isDoneCollecting() {
      for (String description : requiredDescriptions) {
        if (measurementsByDescription.get(description).size() < requiredMeasurements) {
          return false;
        }
      }
      return true;
    }

    @Override
    public boolean isWarmupComplete() {
      return true;
    }

    @Override public ImmutableList<Measurement> getMeasurements() {
      return ImmutableList.copyOf(measurementsByDescription.values());
    }

    @Override public ImmutableList<String> getMessages() {
      return ImmutableList.of();
    }
  }
}
